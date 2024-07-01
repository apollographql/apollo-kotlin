package com.apollographql.apollo.network.ws

import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.toApolloResponse
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.SubscriptionOperationException
import com.apollographql.apollo.internal.CloseableSingleThreadDispatcher
import com.apollographql.apollo.internal.DeferredJsonMerger
import com.apollographql.apollo.internal.isDeferred
import com.apollographql.apollo.internal.transformWhile
import com.apollographql.apollo.network.NetworkTransport
import com.apollographql.apollo.network.ws.internal.Command
import com.apollographql.apollo.network.ws.internal.ConnectionReEstablished
import com.apollographql.apollo.network.ws.internal.Dispose
import com.apollographql.apollo.network.ws.internal.Event
import com.apollographql.apollo.network.ws.internal.GeneralError
import com.apollographql.apollo.network.ws.internal.Message
import com.apollographql.apollo.network.ws.internal.NetworkError
import com.apollographql.apollo.network.ws.internal.OperationComplete
import com.apollographql.apollo.network.ws.internal.OperationError
import com.apollographql.apollo.network.ws.internal.OperationResponse
import com.apollographql.apollo.network.ws.internal.RestartConnection
import com.apollographql.apollo.network.ws.internal.StartOperation
import com.apollographql.apollo.network.ws.internal.StopOperation
import com.benasher44.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import okio.use

/**
 * A [NetworkTransport] that manages a single instance of a [WebSocketConnection].
 *
 * Usually it is used with subscriptions but some [WsProtocol]s like [GraphQLWsProtocol] also support queries and mutations.
 *
 * The [WebSocketConnection] is opened when the first subscription is started and closed if there are no active subscriptions
 * after a given timeout.
 */
class WebSocketNetworkTransport
private constructor(
    private val serverUrl: (suspend () -> String),
    private val headers: List<HttpHeader>,
    private val webSocketEngine: WebSocketEngine = DefaultWebSocketEngine(),
    private val idleTimeoutMillis: Long = 60_000,
    private val protocolFactory: WsProtocol.Factory = SubscriptionWsProtocol.Factory(),
    private val reopenWhen: (suspend (Throwable, attempt: Long) -> Boolean)?
) : NetworkTransport {

  /**
   * The message queue read by the supervisor.
   *
   * SubscriptionFlows write [Command]s
   * The WebSocket coroutine writes [Event]s
   *
   * Use unlimited buffers so that we never have to suspend when writing a command or an event,
   * and we avoid deadlocks. This might be overkill but is most likely never going to be a problem in practice.
   */
  private val messages = Channel<Message>(UNLIMITED)

  /**
   * The SharedFlow read by SubscriptionFlows
   *
   * The Supervisor coroutine writes [Event]s
   */
  private val mutableEvents = MutableSharedFlow<Event>(0, Int.MAX_VALUE, BufferOverflow.SUSPEND)
  private val events = mutableEvents.asSharedFlow()

  val subscriptionCount = mutableEvents.subscriptionCount

  private val backgroundDispatcher = CloseableSingleThreadDispatcher()
  private val coroutineScope = CoroutineScope(backgroundDispatcher.coroutineDispatcher)

  init {
    coroutineScope.launch {
      backgroundDispatcher.use {
        supervise(this)
      }
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
   * Long-running method that creates/handles the websocket lifecycle
   */
  private suspend fun supervise(scope: CoroutineScope) {
    /**
     * No need to lock these variables as they are all accessed from the same thread
     */
    var idleJob: Job? = null
    var connectionJob: Job? = null
    var protocol: WsProtocol? = null
    var reopenAttemptCount = 0L
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
      when (val message = messages.receive()) {
        is Event -> {
          if (message is NetworkError) {
            closeProtocol()

            if (reopenWhen?.invoke(message.cause, reopenAttemptCount) == true) {
              reopenAttemptCount++
              messages.send(RestartConnection)
            } else {
              reopenAttemptCount = 0L
              // forward the NetworkError downstream. Active flows will throw
              mutableEvents.tryEmit(message)
            }
          } else if (message is ConnectionReEstablished) {
            reopenAttemptCount = 0L
            activeMessages.values.forEach {
              // Re-queue all start messages
              // This will restart the websocket
              messages.trySend(it)
            }
          } else {
            reopenAttemptCount = 0L
            mutableEvents.tryEmit(message)
          }
        }

        is Command -> {
          if (message is Dispose) {
            closeProtocol()
            // Exit the loop and the coroutine scope
            return
          }

          if (protocol == null) {
            if (message is StopOperation<*>) {
              // A stop was received, but we don't have a connection. Ignore it
              activeMessages.remove(message.request.requestUuid)
              continue
            }

            val webSocketConnection = try {
              webSocketEngine.open(
                  url = serverUrl(),
                  headers = if (headers.any { it.name == "Sec-WebSocket-Protocol" }) {
                    headers
                  } else {
                    headers + HttpHeader("Sec-WebSocket-Protocol", protocolFactory.name)
                  },
              )
            } catch (e: Exception) {
              // Error opening the websocket
              messages.send(NetworkError(e))
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
              messages.send(NetworkError(e))
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

            is RestartConnection -> {
              messages.send(ConnectionReEstablished())
            }

            else -> {
              // Other cases have been handled above
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
    val deferredJsonMerger = DeferredJsonMerger()

    return events.onSubscription {
      messages.send(StartOperation(request))
    }.filter {
      it.id == request.requestUuid.toString() || it.id == null
    }.transformWhile<Event, Event> {
      when (it) {
        is OperationComplete -> {
          false
        }

        is ConnectionReEstablished -> {
          // means we are in the process of restarting the connection
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
    }.map { response ->
      when (response) {
        is OperationResponse -> {
          val responsePayload = response.payload
          val requestCustomScalarAdapters = request.executionContext[CustomScalarAdapters]!!
          val (payload, mergedFragmentIds) = if (responsePayload.isDeferred()) {
            deferredJsonMerger.merge(responsePayload) to deferredJsonMerger.mergedFragmentIds
          } else {
            responsePayload to null
          }
          val apolloResponse: ApolloResponse<D> = payload.jsonReader().toApolloResponse(
              operation = request.operation,
              requestUuid = request.requestUuid,
              customScalarAdapters = requestCustomScalarAdapters,
              deferredFragmentIdentifiers = mergedFragmentIds
          )

          if (!deferredJsonMerger.hasNext) {
            // Last deferred payload: reset the deferredJsonMerger for potential subsequent responses
            deferredJsonMerger.reset()
          }
          apolloResponse
        }

        is OperationError -> errorResponse(request, SubscriptionOperationException(request.operation.name(), response.payload))
        is NetworkError -> errorResponse(request, ApolloNetworkException("Network error while executing ${request.operation.name()}", response.cause))

        // Cannot happen as these events are filtered out upstream
        is ConnectionReEstablished, is OperationComplete, is GeneralError -> error("Unexpected event $response")
      }
    }.filterNot {
      deferredJsonMerger.isEmptyPayload
    }.onCompletion {
      messages.send(StopOperation(request))
    }
  }

  private fun <D : Operation.Data> errorResponse(
      request: ApolloRequest<D>,
      apolloException: ApolloException,
  ) = ApolloResponse.Builder(requestUuid = request.requestUuid, operation = request.operation)
      .exception(apolloException)
      .isLast(true)
      .build()

  override fun dispose() {
    messages.trySend(Dispose)
  }

  /**
   * Close the connection to the server (if it's open).
   *
   * This can be used to force a reconnection to the server, for instance when new auth tokens should be passed to the headers.
   *
   * The given [reason] will be propagated to [Builder.reopenWhen] to determine if the connection should be reopened. If not, it will be
   * propagated to any Flows waiting for responses.
   */
  fun closeConnection(reason: Throwable) {
    messages.trySend(NetworkError(reason))
  }

  class Builder {
    private var serverUrl: (suspend () -> String)? = null
    private var headers: MutableList<HttpHeader> = mutableListOf()
    private var webSocketEngine: WebSocketEngine? = null
    private var idleTimeoutMillis: Long? = null
    private var protocolFactory: WsProtocol.Factory? = null
    private var reopenWhen: (suspend (Throwable, attempt: Long) -> Boolean)? = null

    /**
     * Configure the server URL.
     *
     * @param serverUrl the url to use to establish the WebSocket connection. It can start with 'https://' or 'wss://' (respectively 'http://'
     * or 'ws://' for unsecure versions), both are handled the same way by the underlying code.
     */
    fun serverUrl(serverUrl: String) = apply {
      this.serverUrl = { serverUrl }
    }

    /**
     * Configure the server URL.
     *
     * @param serverUrl a function return the url to use to establish the WebSocket connection.
     * The url can start with 'https://' or 'wss://' (respectively 'http://'
     * or 'ws://' for unsecure versions), both are handled the same way by the underlying code.
     *
     * [serverUrl] is called every time a WebSocket is opened. For example, you can use it to update your
     * auth credentials in case of an unauthorized error.
     *
     * It is a suspending function, so it can be used to introduce delay before setting the new serverUrl.
     */
    fun serverUrl(serverUrl: (suspend () -> String)?) = apply {
      this.serverUrl = serverUrl
    }

    fun addHeader(name: String, value: String) = apply {
      this.headers += HttpHeader(name, value)
    }

    fun addHeaders(headers: List<HttpHeader>) = apply {
      this.headers.addAll(headers)
    }

    fun headers(headers: List<HttpHeader>) = apply {
      this.headers.clear()
      this.headers.addAll(headers)
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
     * Configure the [WebSocketNetworkTransport] to reopen the websocket automatically when a network error
     * happens
     *
     * @param reopenWhen a function taking the error and attempt index (starting from zero) as parameters and returning 'true' to
     * reopen automatically or 'false' to forward the error to all listening [Flow].
     * It is a suspending function, so it can be used to introduce delay before retry (e.g. backoff strategy).
     *
     */
    fun reopenWhen(reopenWhen: (suspend (Throwable, attempt: Long) -> Boolean)?) = apply {
      this.reopenWhen = reopenWhen
    }

    fun build(): WebSocketNetworkTransport {
      return WebSocketNetworkTransport(
          serverUrl = serverUrl ?: error("No serverUrl specified"),
          headers = headers,
          webSocketEngine = webSocketEngine ?: DefaultWebSocketEngine(),
          idleTimeoutMillis = idleTimeoutMillis ?: 60_000,
          protocolFactory = protocolFactory ?: SubscriptionWsProtocol.Factory(),
          reopenWhen = reopenWhen
      )
    }
  }
}

/**
 * Closes the websocket connection if the transport is a [WebSocketNetworkTransport].
 *
 * @throws IllegalArgumentException if transport is not a [WebSocketNetworkTransport].
 */
fun NetworkTransport.closeConnection(reason: Throwable) {
  (this as? WebSocketNetworkTransport
      ?: throw IllegalArgumentException("'$this' is not an instance of com.apollographql.apollo.ws.WebSocketNetworkTransport")).closeConnection(reason)
}
