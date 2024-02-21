package com.apollographql.apollo3.network.websocket

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.toApolloResponse
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloRetryException
import com.apollographql.apollo3.exception.DefaultApolloException
import com.apollographql.apollo3.internal.DeferredJsonMerger
import com.apollographql.apollo3.network.NetworkTransport
import com.benasher44.uuid.uuid4
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch

/**
 * A [NetworkTransport] that uses WebSockets to execute GraphQL operations. Most of the time, it is used
 * for subscriptions but some [WsProtocol] like [GraphQLWsProtocol] also allow executing queries and mutations
 * over WebSockets.
 *
 * [WebSocketNetworkTransport] supports automatically reconnecting when a network failure happens, see [WebSocketNetworkTransport.Builder.reopenWhen]
 * for more details.
 *
 * @see [WebSocketNetworkTransport.Builder]
 */
class WebSocketNetworkTransport private constructor(
    private val serverUrl: (suspend () -> String),
    private val headers: List<HttpHeader>,
    private val webSocketEngine: WebSocketEngine,
    private val idleTimeoutMillis: Long,
    private val wsProtocolFactory: WsProtocol.Factory,
    private val reopenWhen: (suspend (Throwable, attempt: Long) -> Boolean),
    private val pingIntervalMillis: Long,
    private val connectionAcknowledgeTimeoutMillis: Long,
    private val enableReopen: Boolean,
) : NetworkTransport {

  private val running = atomic(false)
  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
  private val webSocketEvents = Channel<Event>(Channel.UNLIMITED)

  sealed interface Event
  private class StartEvent<D: Operation.Data>(val request: ApolloRequest<D>, val listener: WebSocketOperationListener): Event
  private class StopEvent<D: Operation.Data>(val request: ApolloRequest<D>, val listener: WebSocketOperationListener): Event
  private class DisconnectEvent(val throwable: Throwable, val listeners: List<WebSocketOperationListener>): Event
  private class ConnectedEvent(): Event
  private class CloseEvent(val throwable: Throwable): Event

  private val _isConnected = MutableStateFlow(false)
  @ApolloExperimental
  val isConnected = _isConnected.asStateFlow()

  @Deprecated("This was only used for tests and shouldn't have been exposed", level = DeprecationLevel.ERROR)
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  val subscriptionCount = MutableStateFlow(0)

  private fun onWebSocketConnected() {
    _isConnected.value = true
    webSocketEvents.trySend(ConnectedEvent())
  }

  private fun onWebSocketDisconnected(throwable: Throwable, listeners: List<WebSocketOperationListener>) {
    _isConnected.value = false
    webSocketEvents.trySend(DisconnectEvent(throwable, listeners))
  }

  var attempt = 1L

  private suspend fun CoroutineScope.socketLoop() {
    val socket =  SubscribableWebSocket(
        webSocketEngine = webSocketEngine,
        url = serverUrl(),
        headers = headers,
        idleTimeoutMillis = idleTimeoutMillis,
        onConnected = ::onWebSocketConnected,
        onDisconnected = ::onWebSocketDisconnected,
        dispatcher = Dispatchers.Default,
        wsProtocol = wsProtocolFactory.build(),
        pingIntervalMillis = pingIntervalMillis,
        connectionAcknowledgeTimeoutMillis = connectionAcknowledgeTimeoutMillis,
    )

    while (true) {
      when(val event = webSocketEvents.receive()) {
        is DisconnectEvent -> {
          val cause = if (reopenWhen(event.throwable, attempt)) {
            ApolloRetryException(attempt, event.throwable)
          } else {
              event.throwable
          }

          event.listeners.forEach {
            it.onError(throwable = cause)
          }
          attempt++
          running.value = false
          break
        }
        is ConnectedEvent -> {
          attempt = 1
        }
        is StartEvent<*> -> {
          socket.startOperation(event.request, event.listener)
        }
        is StopEvent<*> -> {
          socket.stopOperation(event.request, event.listener)
        }
        is CloseEvent -> {
          socket.closeConnection(event.throwable)
        }
      }
    }
  }

  /**
   * Executes the given [ApolloRequest] using WebSockets
   *
   * @return a cold [Flow] that subscribes when started and unsubscribes when cancelled.
   * The returned [Flow] buffers responses without upper bound.
   *
   * If [enableReopen] is true and [reopenWhen] returned true, a new subscription with a new uuid will be started
   * on network errors.
   * Else, the [Flow] will emit a response with a non-null [ApolloResponse.exception] and terminate normally.
   */
  override fun <D : Operation.Data> execute(
      request: ApolloRequest<D>,
  ): Flow<ApolloResponse<D>> {

    var renewUuid = false

    val flow = callbackFlow<ApolloResponse<D>> {
      val newRequest = if (renewUuid) {
        request.newBuilder().requestUuid(uuid4()).build()
      } else {
        request
      }
      renewUuid = true

      val operationListener = DefaultWebSocketOperationListener(newRequest, this)

      if (running.compareAndSet(expect = false, update = true)) {
        scope.launch {
          socketLoop()
        }
      }

      webSocketEvents.trySend(StartEvent(newRequest, operationListener))

      awaitClose {
        webSocketEvents.trySend(StopEvent(newRequest, operationListener))
      }
    }

    return flow.buffer(Channel.UNLIMITED).onEach {
      if (it.exception is ApolloRetryException) {
        throw it.exception!!
      }
    }.retryWhen { cause, _ ->
      /**
       * Only retry on [ApolloRetryException], we want to fail in the other programming error cases
       */
      cause is ApolloRetryException
    }
  }

  override fun dispose() {
    closeConnection(DefaultApolloException("The network transport has been disposed"))
    webSocketEvents.close()
  }

  /**
   * Close the connection to the server if it's open.
   *
   * This can be used to force a reconnection to the server, for instance when new auth tokens should be passed to the headers.
   *
   * The given [reason] will be propagated to [Builder.reopenWhen] to determine if the connection should be reopened. If not, it will be
   * propagated to all the [Flow] awaiting responses.
   */
  fun closeConnection(reason: Throwable) {
    webSocketEvents.trySend(CloseEvent(reason))
  }


  class Builder {
    private var serverUrl: (suspend () -> String)? = null
    private var headers: List<HttpHeader>? = null
    private var webSocketEngine: WebSocketEngine? = null
    private var idleTimeoutMillis: Long? = null
    private var reopenWhen: (suspend (Throwable, attempt: Long) -> Boolean)? = null
    private var wsProtocolFactory: WsProtocol.Factory? = null
    private var pingIntervalMillis: Long? = null
    private var connectionAcknowledgeTimeoutMillis: Long? = null
    private var enableReopen = true

    /**
     * @param serverUrl a lambda returning a server url that is called every time a WebSocket
     * connects. The return url must start with:
     *
     * - "ws://"
     * - "wss://"
     * - "http://" (same as "ws://")
     * - "https://" (same as "wss://")
     */
    fun serverUrl(serverUrl: (suspend () -> String)?) = apply {
      this.serverUrl = serverUrl
    }

    /**
     * @param serverUrl a server url that is called every time a WebSocket
     * connects. [serverUrl] must start with:
     *
     * - "ws://"
     * - "wss://"
     * - "http://" (same as "ws://")
     * - "https://" (same as "wss://")
     */
    fun serverUrl(serverUrl: String) = apply {
      this.serverUrl = { serverUrl }
    }

    /**
     * Headers to add to the HTTP handshake query.
     */
    fun headers(headers: List<HttpHeader>) = apply {
      this.headers = headers
    }

    /**
     * Add a [HttpHeader] to the HTTP handshake query.
     */
    fun addHeader(name: String, value: String) = apply {
      this.headers = this.headers.orEmpty() + HttpHeader(name, value)
    }

    /**
     * Set the [WebSocketEngine] to use.
     */
    fun webSocketEngine(webSocketEngine: WebSocketEngine) = apply {
      this.webSocketEngine = webSocketEngine
    }

    /**
     * The number of milliseconds before a WebSocket with no active operations disconnects.
     *
     * Default: `60_000`
     */
    fun idleTimeoutMillis(idleTimeoutMillis: Long) = apply {
      this.idleTimeoutMillis = idleTimeoutMillis
    }

    /**
     * @param reopenWhen a callback that is called every time a network error happens. Return true
     * if the [WebSocketNetworkTransport] should try opening a new WebSocket. This callback can
     * suspend, and it's ok to suspend to implement logic like exponential backoff.
     * [attempt] is the number of consecutive errors. It is reset to 0 after every successful
     * "connection_init" message.
     *
     * Default: `{ false }`
     *
     * @see [closeConnection]
     * @see [enableReopen]
     */
    fun reopenWhen(reopenWhen: (suspend (Throwable, attempt: Long) -> Boolean)?) = apply {
      this.reopenWhen = reopenWhen
    }

    /**
     * @param enableReopen whether to retry by default when `reopenWhen` returns true.
     *
     * Default: true
     *
     * @see [reopenWhen]
     */
    fun enableReopen(enableReopen: Boolean) = apply {
      this.enableReopen = enableReopen
    }

    /**
     * The [WsProtocol.Factory] to use for this [WebSocketNetworkTransport]
     *
     * Default: [GraphQLWsProtocol.Factory]
     *
     * @see [SubscriptionWsProtocol]
     * @see [AppSyncWsProtocol]
     * @see [GraphQLWsProtocol]
     */
    fun wsProtocolFactory(wsProtocolFactory: WsProtocol.Factory) = apply {
      this.wsProtocolFactory = wsProtocolFactory
    }

    @Deprecated("Use wsProtocolFactory instead", ReplaceWith("wsProtocolFactory"), level = DeprecationLevel.ERROR)
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
    fun protocol(wsProtocolFactory: WsProtocol.Factory) = wsProtocolFactory(wsProtocolFactory)

    /**
     * The interval in milliseconds between two client pings or -1 to disable client pings.
     * The [WsProtocol] used must also support client pings.
     *
     * Default: -1
     */
    fun pingIntervalMillis(pingIntervalMillis: Long) = apply {
      this.pingIntervalMillis = pingIntervalMillis
    }

    /**
     * The maximum number of milliseconds between a "connection_init" message and its acknowledgement
     *
     * Default: 10_000
     */
    fun connectionAcknowledgeTimeoutMillis(connectionAcknowledgeTimeoutMillis: Long) = apply {
      this.connectionAcknowledgeTimeoutMillis = connectionAcknowledgeTimeoutMillis
    }

    /**
     * Builds the [WebSocketNetworkTransport]
     */
    fun build() = WebSocketNetworkTransport(
        serverUrl = serverUrl ?: error("You need to set serverUrl"),
        headers = headers ?: emptyList(),
        webSocketEngine = webSocketEngine ?: WebSocketEngine(),
        idleTimeoutMillis = idleTimeoutMillis ?: 60_000,
        reopenWhen = reopenWhen ?: { _, _ -> false },
        wsProtocolFactory = wsProtocolFactory ?: SubscriptionWsProtocol.Factory(),
        pingIntervalMillis = pingIntervalMillis ?: -1L,
        connectionAcknowledgeTimeoutMillis = connectionAcknowledgeTimeoutMillis ?: 10_000L,
        enableReopen = enableReopen
    )
  }
}

private class DefaultWebSocketOperationListener<D : Operation.Data>(
    private val request: ApolloRequest<D>,
    private val producerScope: ProducerScope<ApolloResponse<D>>,
) : WebSocketOperationListener {
  val deferredJsonMerger = DeferredJsonMerger()
  val requestCustomScalarAdapters = request.executionContext[CustomScalarAdapters]!!

  override fun onResponse(response: Any?) {
    @Suppress("UNCHECKED_CAST")
    val responseMap = response as? Map<String, Any?>
    if (responseMap == null) {
      producerScope.trySend(ApolloResponse.Builder(request.operation, request.requestUuid).exception(DefaultApolloException("Invalid payload")).build())
      return
    }
    val (payload, mergedFragmentIds) = if (responseMap.isDeferred()) {
      deferredJsonMerger.merge(responseMap) to deferredJsonMerger.mergedFragmentIds
    } else {
      responseMap to null
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

    if (!deferredJsonMerger.isEmptyPayload) {
      producerScope.trySend(apolloResponse)
    }
  }

  override fun onError(throwable: Throwable) {
    producerScope.trySend(ApolloResponse.Builder(request.operation, request.requestUuid).exception(throwable.wrapIfNeeded("Error while executing operation")).build())
    producerScope.channel.close()
  }

  override fun onComplete() {
    producerScope.channel.close()
  }
}

private fun Map<String, Any?>.isDeferred(): Boolean {
  return keys.contains("hasNext")
}

private fun Throwable.wrapIfNeeded(message: String): ApolloException {
  if (this is ApolloException) {
    return this
  }

  return DefaultApolloException(message, this)
}