package com.apollographql.apollo3.network.ws

import com.apollographql.apollo3.api.AnyAdapter
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.NullableAnyAdapter
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.api.internal.ResponseBodyParser
import com.apollographql.apollo3.api.internal.json.buildJsonByteString
import com.apollographql.apollo3.api.internal.json.buildJsonString
import com.apollographql.apollo3.api.toJson
import com.apollographql.apollo3.internal.BackgroundDispatcher
import com.apollographql.apollo3.network.NetworkTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout


/**
 * A [NetworkTransport] that works with WebSockets. Usually it is used with subscriptions but some [WsProtocol]s like [GraphQLWsProtocol]
 * also support queries and mutations.
 *
 * @param serverUrl the url to use to establish the WebSocket connection. It can start with 'https://' or 'wss://' (respectively 'http://'
 * or 'ws://' for unsecure versions), both are handled the same way by the underlying code.
 * @param webSocketEngine a [WebSocketEngine] that can handle the WebSocket
 *
 */
class WebSocketNetworkTransport(
    private val serverUrl: String,
    private val webSocketEngine: WebSocketEngine = DefaultWebSocketEngine(),
    private val connectionAcknowledgeTimeoutMs: Long = 10_000,
    private val idleTimeoutMillis: Long = 60_000,
    private val protocol: WsProtocol = SubscriptionWsProtocol(),
) : NetworkTransport {

  constructor(
      serverUrl: String,
      connectionAcknowledgeTimeoutMs: Long = 10_000,
      idleTimeoutMillis: Long = 60_000,
      protocol: WsProtocol = SubscriptionWsProtocol(),
  ) : this(
      serverUrl,
      DefaultWebSocketEngine(),
      connectionAcknowledgeTimeoutMs,
      idleTimeoutMillis,
      protocol
  )

  private val sendMessage: suspend WebSocketConnection.(Map<String, Any?>) -> Unit

  private interface Command
  class StartOperation<D : Operation.Data>(val request: ApolloRequest<D>) : Command
  class StopOperation<D : Operation.Data>(val request: ApolloRequest<D>) : Command

  private interface Event {
    val id: String
  }

  private class OperationData(override val id: String, val payload: Map<String, Any?>) : Event
  private class OperationError(override val id: String, val cause: Throwable) : Event
  private class OperationComplete(override val id: String) : Event

  private val commands = Channel<Command>(64)
  private val mutableEvents = MutableSharedFlow<Event>(0, 64, BufferOverflow.SUSPEND)
  private val events = mutableEvents.asSharedFlow()

  val subscriptionCount = mutableEvents.subscriptionCount

  private val backgroundDispatcher = BackgroundDispatcher()
  private val coroutineScope = CoroutineScope(backgroundDispatcher.coroutineDispatcher)

  init {
    /**
     * When receiving, we can always convert as required.
     * When sending, some servers might be sensitive to the type of frame so
     * this is configurable
     */
    sendMessage = when (protocol.frameType) {
      WsFrameType.Binary -> { message ->
        send(message.toByteString())
      }
      WsFrameType.Text -> { message ->
        send(message.toUtf8())
      }
    }
    coroutineScope.launch {
      var currentConnection: WebSocketConnection? = null
      var readJob: Job? = null
      var idleJob: Job? = null
      val subscribers = mutableSetOf<String>()

      while (true) {
        when (val command = commands.receive()) {
          is StartOperation<*> -> {
            idleJob?.cancel()
            subscribers.add(command.request.requestUuid.toString())
            if (currentConnection == null) {
              try {
                currentConnection = createConnection()
              } catch (e: Exception) {
                subscribers.forEach {
                  mutableEvents.emit(
                      OperationError(it, e)
                  )
                }
              }

              if (currentConnection != null) {
                readJob?.cancel()
                readJob = launch {
                  try {
                    readWebSocket(currentConnection!!)
                  } catch (e: Exception) {
                    subscribers.forEach {
                      mutableEvents.emit(
                          OperationError(it, e)
                      )
                    }
                    currentConnection?.close()
                    currentConnection = null
                  }
                }
              }
            }
            currentConnection?.sendMessage(protocol.operationStart(command.request))
          }
          is StopOperation<*> -> {
            subscribers.remove(command.request.requestUuid.toString())
            currentConnection?.sendMessage(protocol.operationStop(command.request))
            if (subscribers.isEmpty()) {
              idleJob?.cancel()
              idleJob = launch {
                delay(idleTimeoutMillis)
                check(subscribers.isEmpty())
                protocol.connectionTerminate()?.let {
                  currentConnection?.sendMessage(it)
                }
                currentConnection?.close()
                currentConnection = null
              }
            }
          }
        }
      }
    }
  }

  private suspend fun readWebSocket(webSocketConnection: WebSocketConnection) {
    while (true) {
      val bytes = webSocketConnection.receive()

      val wsMessage = protocol.parseMessage(bytes.utf8(), webSocketConnection)
      val event = when (wsMessage) {
        is WsMessage.OperationData -> OperationData(wsMessage.id, wsMessage.payload)
        is WsMessage.OperationError -> OperationError(wsMessage.id, ApolloNetworkException("Cannot execute operation: ${wsMessage.payload}"))
        is WsMessage.OperationComplete -> OperationComplete(wsMessage.id)
        is WsMessage.Unknown -> null
        is WsMessage.KeepAlive -> null // should we acknowledge the keepalive somehow here?
        is WsMessage.ConnectionAck -> null // should not happen at this point
        is WsMessage.ConnectionError -> null // should not happen at this point
      }
      if (event != null) {
        mutableEvents.emit(event)
      }
    }
  }

  private fun Map<String, Any?>.toByteString() = buildJsonByteString {
    AnyAdapter.toJson(this, this@toByteString)
  }

  private fun Map<String, Any?>.toUtf8() = buildJsonString {
    AnyAdapter.toJson(this, this@toUtf8)
  }

  private suspend fun createConnection(): WebSocketConnection {
    val webSocketConnection = webSocketEngine.open(
        url = serverUrl,
        headers = mapOf(
            "Sec-WebSocket-Protocol" to protocol.name,
        )
    )
    try {
      withTimeout(connectionAcknowledgeTimeoutMs) {
        webSocketConnection.sendMessage(protocol.connectionInit())
        while (true) {
          val payload = webSocketConnection.receive()

          when (val message = protocol.parseMessage(payload.utf8(), webSocketConnection)) {
            is WsMessage.ConnectionAck -> return@withTimeout null
            is WsMessage.ConnectionError -> throw ApolloNetworkException("Server error when connecting to $serverUrl: ${NullableAnyAdapter.toJson(message.payload)}")
            else -> Unit // unknown message?
          }
        }
      }
    } catch (e: TimeoutCancellationException) {
      throw ApolloNetworkException("Ack not received after $connectionAcknowledgeTimeoutMs millis when connecting to $serverUrl")
    } catch (e: Exception) {
      throw ApolloNetworkException("Error while connecting to $serverUrl", e)
    }

    return webSocketConnection
  }

  override fun <D : Operation.Data> execute(
      request: ApolloRequest<D>,
  ): Flow<ApolloResponse<D>> {
    return events.onSubscription {
      commands.send(StartOperation(request))
    }.filter {
      it.id == request.requestUuid.toString()
    }.takeWhile {
      it !is OperationComplete
    }.map {
      when (it) {
        is OperationData -> ResponseBodyParser.parse(
            it.payload,
            request.operation,
            request.executionContext[CustomScalarAdapters]!!
        ).copy(requestUuid = request.requestUuid)
        is OperationError -> throw it.cause
        else -> error("Unsupported event $it")
      }
    }.onCompletion {
      commands.send(StopOperation(request))
    }
  }
  override fun dispose() {
    coroutineScope.cancel()
    backgroundDispatcher.dispose()
  }
}

