package com.apollographql.apollo3.network.websocket

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloExperimental
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
import com.apollographql.apollo3.network.websocket.internal.OperationListener
import com.apollographql.apollo3.network.websocket.internal.WebSocketHolder
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
@ApolloExperimental
class WebSocketNetworkTransport private constructor(
    private val webSocketEngine: WebSocketEngine,
    private val serverUrl: String,
    private val httpHeaders: List<HttpHeader>,
    private val wsProtocol: WsProtocol,
    private val connectionAcknowledgeTimeoutMillis: Long,
    private val pingIntervalMillis: Long,
    private val idleTimeoutMillis: Long,
    private val parserFactory: SubscriptionParserFactory
) : NetworkTransport {

  private val holder = WebSocketHolder(
      webSocketEngine = webSocketEngine,
      serverUrl = serverUrl,
      httpHeaders = httpHeaders,
      wsProtocol = wsProtocol,
      connectionAcknowledgeTimeoutMillis = connectionAcknowledgeTimeoutMillis,
      pingIntervalMillis = pingIntervalMillis,
      idleTimeoutMillis = idleTimeoutMillis
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

      val operationListener = DefaultOperationListener(newRequest, this, parserFactory.createParser(request))

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

  @ApolloExperimental
  class Builder {
    private var serverUrl: String? = null
    private var httpHeaders: List<HttpHeader>? = null
    private var webSocketEngine: WebSocketEngine? = null
    private var wsProtocol: WsProtocol? = null
    private var connectionAcknowledgeTimeoutMillis: Long? = null
    private var pingIntervalMillis: Long? = null
    private var idleTimeoutMillis: Long? = null
    private var parserFactory: SubscriptionParserFactory? = null

    /**
     * @param serverUrl a server url that is called every time a WebSocket
     * connects. [serverUrl] must start with:
     *
     * - "ws://"
     * - "wss://"
     * - "http://" (same as "ws://")
     * - "https://" (same as "wss://")
     */
    fun serverUrl(serverUrl: String?) = apply {
      this.serverUrl = serverUrl
    }

    /**
     * Headers to add to the HTTP handshake query.
     */
    fun httpHeaders(headers: List<HttpHeader>) = apply {
      this.httpHeaders = headers
    }

    /**
     * Add a [HttpHeader] to the handshake query.
     */
    fun addHttpHeader(name: String, value: String) = apply {
      this.httpHeaders = this.httpHeaders.orEmpty() + HttpHeader(name, value)
    }

    /**
     * Add a [HttpHeader] to the handshake query.
     */
    @Deprecated("use addHttpHeader instead", ReplaceWith("addHttpHeader(name, value)"))
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
    fun addHeader(name: String, value: String) = apply {
      this.httpHeaders = this.httpHeaders.orEmpty() + HttpHeader(name, value)
    }

    /**
     * Set the [WebSocketEngine] to use.
     */
    fun webSocketEngine(webSocketEngine: WebSocketEngine?) = apply {
      this.webSocketEngine = webSocketEngine
    }

    /**
     * The number of milliseconds before a WebSocket with no active operations disconnects.
     *
     * Default: `60_000`
     */
    fun idleTimeoutMillis(idleTimeoutMillis: Long?) = apply {
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
    fun wsProtocol(wsProtocol: WsProtocol?) = apply {
      this.wsProtocol = wsProtocol
    }

    /**
     * The interval in milliseconds between two client pings or -1 to disable client pings.
     * The [WsProtocol] used must also support client pings.
     *
     * Default: -1
     */
    fun pingIntervalMillis(pingIntervalMillis: Long?) = apply {
      this.pingIntervalMillis = pingIntervalMillis
    }

    /**
     * The maximum number of milliseconds between a "connection_init" message and its acknowledgement
     *
     * Default: 10_000
     */
    fun connectionAcknowledgeTimeoutMillis(connectionAcknowledgeTimeoutMillis: Long?) = apply {
      this.connectionAcknowledgeTimeoutMillis = connectionAcknowledgeTimeoutMillis
    }

    @ApolloExperimental
    fun parserFactory(parserFactory: SubscriptionParserFactory?) = apply {
      this.parserFactory = parserFactory
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
          parserFactory = parserFactory ?: DefaultSubscriptionParserFactory
      )
    }
  }
}

private object DefaultSubscriptionParserFactory: SubscriptionParserFactory {
  override fun <D : Operation.Data> createParser(request: ApolloRequest<D>): SubscriptionParser<D> {
    return DefaultSubscriptionParser(request)
  }
}

private class DefaultSubscriptionParser<D : Operation.Data>(private val request: ApolloRequest<D>) : SubscriptionParser<D> {
  private var deferredJsonMerger: DeferredJsonMerger = DeferredJsonMerger()
  private val requestCustomScalarAdapters = request.executionContext[CustomScalarAdapters] ?: CustomScalarAdapters.Empty

  @Suppress("NAME_SHADOWING")
  override fun parse(response: ApolloJsonElement): ApolloResponse<D>? {
    @Suppress("UNCHECKED_CAST")
    val responseMap = response as? Map<String, Any?>
    if (responseMap == null) {
      return ApolloResponse.Builder(request.operation, request.requestUuid)
          .exception(DefaultApolloException("Invalid payload")).build()
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

    if (deferredJsonMerger.isEmptyPayload) {
      return null
    } else {
      return apolloResponse
    }
  }
}

private class DefaultOperationListener<D : Operation.Data>(
    private val request: ApolloRequest<D>,
    private val producerScope: ProducerScope<ApolloResponse<D>>,
    private val parser: SubscriptionParser<D>
) : OperationListener {
  override fun onResponse(response: ApolloJsonElement) {
    parser.parse(response)?.let {
      producerScope.trySend(it)
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

  override fun onError(payload: ApolloJsonElement) {
    producerScope.trySend(errorResponse(SubscriptionOperationException(request.operation.name(), payload)))
    producerScope.close()
  }

  override fun onTransportError(cause: ApolloException) {
    producerScope.trySend(errorResponse(cause))
    producerScope.close()
  }
}

private fun Map<String, Any?>.isDeferred(): Boolean {
  return keys.contains("hasNext")
}

/**
 * Closes the websocket connection if the transport is a [WebSocketNetworkTransport].
 *
 * [exception] is passed down to [ApolloResponse.exception] so you can decide how to handle the exception for active subscriptions.
 *
 * ```
 * apolloClient.subscriptionNetworkTransport.closeConnection(DefaultApolloException("oh no!"))
 * ```
 *
 * @throws IllegalArgumentException if transport is not a [WebSocketNetworkTransport]
 * @see DefaultApolloException
 */
@ApolloExperimental
fun NetworkTransport.closeConnection(exception: ApolloException) {
  val webSocketNetworkTransport = (this as? WebSocketNetworkTransport) ?: throw IllegalArgumentException("'$this' is not an instance of com.apollographql.apollo3.websocket.WebSocketNetworkTransport")

  webSocketNetworkTransport.closeConnection(exception)
}
