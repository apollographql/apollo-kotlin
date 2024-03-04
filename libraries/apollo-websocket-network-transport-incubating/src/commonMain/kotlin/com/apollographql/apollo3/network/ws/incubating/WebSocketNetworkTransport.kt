package com.apollographql.apollo3.network.ws.incubating

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.json.ApolloJsonElement
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.toApolloResponse
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.DefaultApolloException
import com.apollographql.apollo3.exception.SubscriptionOperationException
import com.apollographql.apollo3.internal.DeferredJsonMerger
import com.apollographql.apollo3.network.NetworkTransport
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

/**
 * A [NetworkTransport] that uses WebSockets to execute GraphQL operations. Most of the time, it is used
 * for subscriptions but some [WsProtocol] like [GraphQLWsProtocol] also allow executing queries and mutations
 * over WebSockets.
 *
 * @see [WebSocketNetworkTransport.Builder]
 */
class WebSocketNetworkTransport private constructor(
    private val webSocketEngine: WebSocketEngine,
    private val serverUrl: String,
    private val httpHeaders: List<HttpHeader>,
    private val wsProtocol: WsProtocol,
    private val connectionAcknowledgeTimeoutMillis: Long,
    private val pingIntervalMillis: Long,
    private val idleTimeoutMillis: Long,
) : NetworkTransport {

  private val holder = WebSocketHolder(
      webSocketEngine,
      serverUrl,
      httpHeaders,
      wsProtocol,
      connectionAcknowledgeTimeoutMillis,
      pingIntervalMillis,
      idleTimeoutMillis
  )

  /**
   * Executes the given [ApolloRequest] using WebSockets
   *
   * @return a cold [Flow] that subscribes when started and unsubscribes when cancelled.
   * The returned [Flow] buffers responses without upper bound.
   *
   * Else, the [Flow] will emit a response with a non-null [ApolloResponse.exception] and terminate normally.
   */
  override fun <D : Operation.Data> execute(
      request: ApolloRequest<D>,
  ): Flow<ApolloResponse<D>> {

    var renewUuid = false

    val flow = callbackFlow {
      val newRequest = if (renewUuid) {
        request.newBuilder().requestUuid(uuid4()).build()
      } else {
        request
      }
      renewUuid = true

      val operationListener = DefaultOperationListener(newRequest, this)

      val webSocket = holder.acquire()
      webSocket.startOperation(newRequest, operationListener)

      awaitClose {
        webSocket.stopOperation(newRequest)
      }
    }

    // buffer because we're emitting from websocket callbacks and we can't suspend there
    return flow.buffer(Channel.UNLIMITED)
  }

  override fun dispose() {
    holder.close()
  }

  /**
   * Close the connection to the server if it's open.
   *
   * This can be used to force a reconnection to the server, for instance when new auth tokens should be passed to the headers.
   *
   * The given [reason] will be propagated to active subscriptions.
   */
  fun closeConnection(reason: ApolloException) {
    holder.closeCurrentConnection(reason)
  }


  class Builder {
    private var serverUrl: String? = null
    private var httpHeaders: List<HttpHeader>? = null
    private var webSocketEngine: WebSocketEngine? = null
    private var wsProtocol: WsProtocol? = null
    private var connectionAcknowledgeTimeoutMillis: Long? = null
    private var pingIntervalMillis: Long? = null
    private var idleTimeoutMillis: Long? = null

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
      this.serverUrl = serverUrl
    }

    /**
     * Headers to add to the HTTP handshake query.
     */
    fun httpHeaders(headers: List<HttpHeader>) = apply {
      this.httpHeaders = headers
    }

    /**
     * Headers to add to the HTTP handshake query.
     */
    fun addHttpHeaders(name: String, value: String) = apply {
      this.httpHeaders = this.httpHeaders.orEmpty() + HttpHeader(name, value)
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
     * The [WsProtocol] to use for this [WebSocketNetworkTransport]
     *
     * Default: [GraphQLWsProtocol]
     *
     * @see [SubscriptionWsProtocol]
     * @see [AppSyncWsProtocol]
     * @see [GraphQLWsProtocol]
     */
    fun wsProtocol(wsProtocol: WsProtocol) = apply {
      this.wsProtocol = wsProtocol
    }

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
    fun build(): WebSocketNetworkTransport {
      return WebSocketNetworkTransport(
          webSocketEngine = webSocketEngine ?: WebSocketEngine(),
          serverUrl = serverUrl ?: error("Apollo: 'serverUrl' is required"),
          httpHeaders = httpHeaders.orEmpty(),
          idleTimeoutMillis = idleTimeoutMillis ?: 60_000,
          wsProtocol = wsProtocol ?: GraphQLWsProtocol { null },
          pingIntervalMillis = pingIntervalMillis ?: -1L,
          connectionAcknowledgeTimeoutMillis = connectionAcknowledgeTimeoutMillis ?: 10_000L,
      )
    }
  }
}

private class DefaultOperationListener<D : Operation.Data>(
    private val request: ApolloRequest<D>,
    private val producerScope: ProducerScope<ApolloResponse<D>>,
) : OperationListener {
  val deferredJsonMerger = DeferredJsonMerger()
  val requestCustomScalarAdapters = request.executionContext[CustomScalarAdapters]!!

  override fun onResponse(response: Any?) {
    @Suppress("UNCHECKED_CAST")
    val responseMap = response as? Map<String, Any?>
    if (responseMap == null) {
      producerScope.trySend(ApolloResponse.Builder(request.operation, request.requestUuid)
          .exception(DefaultApolloException("Invalid payload")).build()
      )
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

  override fun onComplete() {
    producerScope.close()
  }

  private fun errorResponse(cause: ApolloException): ApolloResponse<D> {
    return ApolloResponse.Builder(request.operation, request.requestUuid)
        .exception(cause)
        .build()
  }

  override fun onError(payload: ApolloJsonElement, terminal: Boolean) {
    producerScope.trySend(errorResponse(SubscriptionOperationException(request.operation.name(), payload)))
    if (terminal) {
      producerScope.close()
    }
  }

  override fun onTransportError(cause: ApolloException) {
    producerScope.trySend(errorResponse(cause))
    producerScope.close()
  }
}

private fun Map<String, Any?>.isDeferred(): Boolean {
  return keys.contains("hasNext")
}
