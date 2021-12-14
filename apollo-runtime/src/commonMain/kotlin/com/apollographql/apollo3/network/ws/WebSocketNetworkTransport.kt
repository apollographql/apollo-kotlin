package com.apollographql.apollo3.network.ws

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.internal.BackgroundDispatcher
import com.apollographql.apollo3.internal.transformWhile
import com.apollographql.apollo3.network.NetworkTransport
import com.apollographql.apollo3.network.ws.internal.Command
import com.apollographql.apollo3.network.ws.internal.Dispose
import com.apollographql.apollo3.network.ws.internal.Event
import com.apollographql.apollo3.network.ws.internal.GeneralError
import com.apollographql.apollo3.network.ws.internal.Message
import com.apollographql.apollo3.network.ws.internal.NetworkError
import com.apollographql.apollo3.network.ws.internal.OperationComplete
import com.apollographql.apollo3.network.ws.internal.OperationError
import com.apollographql.apollo3.network.ws.internal.OperationResponse
import com.apollographql.apollo3.network.ws.internal.StartOperation
import com.apollographql.apollo3.network.ws.internal.StopOperation
import com.benasher44.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch

/**
 * A [NetworkTransport] that works with WebSockets. Usually it is used with subscriptions but some [WsProtocol]s like [GraphQLWsProtocol]
 * also support queries and mutations.
 *
 * @param serverUrl the url to use to establish the WebSocket connection. It can start with 'https://' or 'wss://' (respectively 'http://'
 * or 'ws://' for unsecure versions), both are handled the same way by the underlying code.
 * @param webSocketEngine a [WebSocketEngine] that can handle the WebSocket
 *
 */
class WebSocketNetworkTransport
private constructor(
    private val serverUrl: String,
    private val webSocketEngine: WebSocketEngine = DefaultWebSocketEngine(),
    private val idleTimeoutMillis: Long = 60_000,
    private val protocolFactory: WsProtocol.Factory = SubscriptionWsProtocol.Factory(),
    private val reconnectWhen: ((Throwable) -> Boolean)?,
) : NetworkTransport {

  /**
   * Use unlimited buffers so that we never have to suspend when writing a command or an event,
   * and we avoid deadlocks. This might be overkill but is most likely never going to be a problem in practice.
   */
  private val messages = Channel<Message>(UNLIMITED)

  /**
   * This takes messages from [messages] and broadcasts the [Event]s
   */
  private val mutableEvents = MutableSharedFlow<Event>(0, Int.MAX_VALUE, BufferOverflow.SUSPEND)
  private val events = mutableEvents.asSharedFlow()

  val subscriptionCount = mutableEvents.subscriptionCount

  private val backgroundDispatcher = BackgroundDispatcher()
  private val coroutineScope = CoroutineScope(backgroundDispatcher.coroutineDispatcher)

  init {
    coroutineScope.launch {
      supervise(this)
      backgroundDispatcher.dispose()
    }
  }

  private val listener = object : WsProtocol.Listener {
    override fun operationResponse(id: String, payload: Map<String, Any?>) {
      messages.trySend(OperationResponse(id, payload))
    }

    override fun operationError(id: String, payload: Map<String, Any?>?) {
      messages.trySend(OperationError(id, payload))
    }

    override fun operationComplete(id: String) {
      messages.trySend(OperationComplete(id))
    }

    override fun generalError(payload: Map<String, Any?>?) {
      messages.trySend(GeneralError(payload))
    }

    override fun networkError(cause: Throwable) {
      messages.trySend(NetworkError(cause))
    }
  }

  /**
   * Long-running method that creates/handles the websocket lifecyle
   */
  private suspend fun supervise(scope: CoroutineScope) {
    /**
     * No need to lock these variables as they are all accessed from the same thread
     */
    var idleJob: Job? = null
    var connectionJob: Job? = null
    var protocol: WsProtocol? = null
    val activeMessages = mutableMapOf<Uuid, StartOperation<*>>()

    /**
     * This happens:
     * - when this coroutine receives a [Dispose] message
     * - when the idleJob completes
     * - when there is an error reading the WebSocket and this coroutine receives a [NetworkError] message
     */
    fun closeProtocol() {
      protocol?.close()
      protocol = null
      connectionJob?.cancel()
      connectionJob = null
      idleJob?.cancel()
      idleJob = null
    }

    while (true) {
      val message = messages.receive()

      when (message) {
        is Event -> {
          if (message is NetworkError) {
            closeProtocol()

            if (reconnectWhen?.invoke(message.cause) == true) {
              activeMessages.values.forEach {
                // Re-queue all start messages
                // This will restart the websocket
                messages.trySend(it)
              }
            } else {
              // forward the NetworkError downstream. Active flows will throw
              mutableEvents.tryEmit(message)
            }
          } else {
            mutableEvents.tryEmit(message)
          }
        }
        is Command -> {
          if (protocol == null) {
            if (message !is StartOperation<*>) {
              // A stop was received, but we don't have a connection. Ignore it
              continue
            }

            val webSocketConnection = try {
              webSocketEngine.open(
                  url = serverUrl,
                  headers = mapOf(
                      "Sec-WebSocket-Protocol" to protocolFactory.name,
                  )
              )
            } catch (e: Exception) {
              // Error opening the websocket
              mutableEvents.emit(NetworkError(e))
              continue
            }

            protocol = protocolFactory.create(
                webSocketConnection = webSocketConnection,
                listener = listener,
                scope = scope,
            )
            try {
              protocol!!.connectionInit()
            } catch (e: Exception) {
              // Error initializing the connection
              protocol = null
              mutableEvents.emit(NetworkError(e))
              continue
            }

            /**
             * We start as [CoroutineStart.UNDISPATCHED] to make sure protocol.run() is always be called.
             * I'm not 100% sure if protocol could be reset before starting to execute the coroutine
             * so added this as an extra precaution. Maybe it's not required, but it shouldn't hurt
             * so better safe than sorry...
             */
            connectionJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
              protocol!!.run()
            }
          }

          when (message) {
            is StartOperation<*> -> {
              activeMessages[message.request.requestUuid] = message
              protocol!!.startOperation(message.request)
            }
            is StopOperation<*> -> {
              activeMessages.remove(message.request.requestUuid)
              protocol!!.stopOperation(message.request)
            }
            is Dispose -> {
              closeProtocol()
              // Exit the loop and the coroutine scope
              return
            }
          }

          if (activeMessages.isEmpty()) {
            idleJob = scope.launch {
              delay(idleTimeoutMillis)
              closeProtocol()
            }
          } else {
            idleJob?.cancel()
            idleJob = null
          }
        }
      }
    }
  }

  override fun <D : Operation.Data> execute(
      request: ApolloRequest<D>,
  ): Flow<ApolloResponse<D>> {
    return events.onSubscription {
      messages.send(StartOperation(request))
    }.filter {
      it.id == request.requestUuid.toString() || it.id == null
    }.transformWhile<Event, Event> {
      when (it) {
        is OperationComplete -> {
          false
        }
        is NetworkError -> {
          emit(it)
          false
        }
        is GeneralError -> {
          // The server sends an error without an operation id. This happens when sending an unknown message type
          // to https://apollo-fullstack-tutorial.herokuapp.com/ for an example. In that case, this error is not fatal
          // and the server will continue honouring other subscriptions, so we just filter the error out and log it.
          println("Received general error while executing operation ${request.operation.name()}: ${it.payload}")
          true
        }
        else -> {
          emit(it)
          true
        }
      }
    }.map {
      when (it) {
        is OperationResponse -> request.operation
            .parseJsonResponse(it.payload.jsonReader(), request.executionContext[CustomScalarAdapters]!!)
            .newBuilder()
            .requestUuid(request.requestUuid)
            .build()
        is OperationError -> throw ApolloNetworkException("Operation error ${request.operation.name()}: ${it.payload}")
        is NetworkError -> throw ApolloNetworkException("Network error while executing ${request.operation.name()}", it.cause)

        // Cannot happen as these events are filtered out upstream
        is OperationComplete, is GeneralError -> error("Unexpected event $it")
      }
    }.onCompletion {
      messages.send(StopOperation(request))
    }
  }

  override fun dispose() {
    messages.trySend(Dispose)
  }

  class Builder {
    private var serverUrl: String? = null
    private var webSocketEngine: WebSocketEngine? = null
    private var idleTimeoutMillis: Long? = null
    private var protocolFactory: WsProtocol.Factory? = null
    private var reconnectWhen: ((Throwable) -> Boolean)? = null

    fun serverUrl(serverUrl: String) = apply {
      this.serverUrl = serverUrl
    }

    @Suppress("DEPRECATION")
    fun webSocketEngine(webSocketEngine: WebSocketEngine) = apply {
      this.webSocketEngine = webSocketEngine
    }

    fun idleTimeoutMillis(idleTimeoutMillis: Long) = apply {
      this.idleTimeoutMillis = idleTimeoutMillis
    }

    fun protocol(protocolFactory: WsProtocol.Factory) = apply {
      this.protocolFactory = protocolFactory
    }

    /**
     * Configure the [WebSocketNetworkTransport] to reconnect the websocket automatically when a network error
     * happens
     *
     * @param reconnectWhen a function taking the error as a parameter and returning 'true' to reconnect
     * automatically or 'false' to forward the error to all listening [Flow]
     *
     */
    fun reconnectWhen(reconnectWhen: ((Throwable) -> Boolean)?) = apply {
      this.reconnectWhen = reconnectWhen
    }

    fun build(): WebSocketNetworkTransport {
      @Suppress("DEPRECATION")
      return WebSocketNetworkTransport(
          serverUrl ?: error("No serverUrl specified"),
          webSocketEngine ?: DefaultWebSocketEngine(),
          idleTimeoutMillis ?: 60_000,
          protocolFactory ?: SubscriptionWsProtocol.Factory(),
          reconnectWhen
      )
    }
  }
}
