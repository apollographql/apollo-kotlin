package com.apollographql.apollo.network.ws

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloDeprecatedSince.Version.v3_7_2
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.http.DefaultWebSocketPayloadComposer
import com.apollographql.apollo.api.http.WebSocketPayloadComposer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout


/**
 * An [WsProtocol] that uses https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md
 * It can carry queries in addition to subscriptions over the websocket
 */
class GraphQLWsProtocol internal constructor(
    private val connectionPayload: suspend () -> Map<String, Any?>? = { null },
    private val pingPayload: Map<String, Any?>? = null,
    private val pongPayload: Map<String, Any?>? = null,
    private val connectionAcknowledgeTimeoutMs: Long,
    private val pingIntervalMillis: Long,
    private val frameType: WsFrameType,
    webSocketConnection: WebSocketConnection,
    listener: Listener,
    private val scope: CoroutineScope,
    private val webSocketPayloadComposer: WebSocketPayloadComposer,
) : WsProtocol(webSocketConnection, listener) {

  @Deprecated("Use GraphQLWsProtocol.Factory instead")
  @ApolloDeprecatedSince(v3_7_2)
  constructor(
      connectionPayload: suspend () -> Map<String, Any?>? = { null },
      pingPayload: Map<String, Any?>? = null,
      pongPayload: Map<String, Any?>? = null,
      connectionAcknowledgeTimeoutMs: Long,
      pingIntervalMillis: Long,
      frameType: WsFrameType,
      webSocketConnection: WebSocketConnection,
      listener: Listener,
      scope: CoroutineScope,
  ) : this(
      connectionPayload, pingPayload, pongPayload, connectionAcknowledgeTimeoutMs, pingIntervalMillis, frameType, webSocketConnection, listener, scope, DefaultWebSocketPayloadComposer()
  )

  override suspend fun connectionInit() {
    val message = mutableMapOf<String, Any?>(
        "type" to "connection_init",
    )

    val payload = connectionPayload()
    if (payload != null) {
      message.put("payload", payload)
    }

    sendMessageMap(message, frameType)

    withTimeout(connectionAcknowledgeTimeoutMs) {
      val map = receiveMessageMap()
      when (val type = map["type"]) {
        "connection_ack" -> return@withTimeout
        "ping" -> sendPong()
        else -> println("unknown graphql-ws message while waiting for connection_ack: '$type")
      }
    }
  }

  override fun <D : Operation.Data> startOperation(request: ApolloRequest<D>) {
    sendMessageMap(
        mapOf(
            "type" to "subscribe",
            "id" to request.requestUuid.toString(),
            "payload" to webSocketPayloadComposer.compose(request)
        ),
        frameType
    )
  }

  override fun <D : Operation.Data> stopOperation(request: ApolloRequest<D>) {
    sendMessageMap(
        mapOf(
            "type" to "complete",
            "id" to request.requestUuid.toString()
        ),
        frameType
    )
  }

  override suspend fun run() {
    if (pingIntervalMillis > 0) {
      scope.launch {
        val map = mutableMapOf<String, Any?>(
            "type" to "ping",
        )
        if (pingPayload != null) {
          map["payload"] = pingPayload
        }

        while (true) {
          delay(pingIntervalMillis)
          sendMessageMap(map, frameType)
        }
      }
    }
    super.run()
  }

  override fun handleServerMessage(messageMap: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    when (messageMap["type"]) {
      "next" -> listener.operationResponse(messageMap["id"] as String, messageMap["payload"] as Map<String, Any?>)
      "error" -> {
        // GraphQL WS errors payloads are actually List<GraphQLError>. Pass them as usual responses
        // See https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md#error
        // See https://github.com/apollographql/apollo-kotlin/issues/4538
        listener.operationResponse(messageMap["id"] as String, mapOf("errors" to messageMap["payload"]))
        listener.operationComplete(messageMap["id"] as String)
      }

      "complete" -> listener.operationComplete(messageMap["id"] as String)
      "ping" -> {
        sendPong()
      }

      "pong" -> Unit // Nothing to do, the server acknowledged one of our pings
      else -> Unit // Unknown message
    }
  }

  private fun sendPong() {
    val map = mutableMapOf<String, Any?>(
        "type" to "pong",
    )
    if (pongPayload != null) {
      map["payload"] = pongPayload
    }
    sendMessageMap(map, frameType)
  }

  /**
   * A factory for [GraphQLWsProtocol].
   *
   */
  class Factory constructor() : WsProtocol.Factory {
    private var connectionPayload: (suspend () -> Map<String, Any?>?)? = null
    private var pingIntervalMillis: Long? = null
    private var pingPayload: Map<String, Any?>? = null
    private var pongPayload: Map<String, Any?>? = null
    private var connectionAcknowledgeTimeoutMs: Long? = null
    private var frameType: WsFrameType? = null
    private var webSocketPayloadComposer: WebSocketPayloadComposer? = null

    /**
     * @param connectionPayload a map of additional parameters to send in the "connection_init" message
     * @param pingIntervalMillis the interval between two client-initiated pings or -1 to not send any ping.
     * Default value: -1
     * @param pingPayload the ping payload to send in "ping" messages or null to not send a payload
     * @param pongPayload the pong payload to send in "pong" messages or null to not send a payload
     * @param connectionAcknowledgeTimeoutMs the timeout for receiving the "connection_ack" message, in milliseconds
     * @param frameType the type of the websocket frames to use. Default value: [WsFrameType.Text]
     */
    constructor(
        connectionPayload: suspend () -> Map<String, Any?>? = { null },
        pingIntervalMillis: Long = -1,
        pingPayload: Map<String, Any?>? = null,
        pongPayload: Map<String, Any?>? = null,
        connectionAcknowledgeTimeoutMs: Long = 10_000,
        frameType: WsFrameType = WsFrameType.Text,
    ) : this() {
      this.connectionPayload = connectionPayload
      this.pingIntervalMillis = pingIntervalMillis
      this.pingPayload = pingPayload
      this.pongPayload = pongPayload
      this.connectionAcknowledgeTimeoutMs = connectionAcknowledgeTimeoutMs
      this.frameType = frameType
    }

    @ApolloExperimental
    fun connectionPayload(connectionPayload: suspend () -> Map<String, Any?>) {
      this.connectionPayload = connectionPayload
    }

    @ApolloExperimental
    fun pingIntervalMillis(pingIntervalMillis: Long) {
      this.pingIntervalMillis = pingIntervalMillis
    }

    @ApolloExperimental
    fun pingPayload(pingPayload: Map<String, Any?>?) {
      this.pingPayload = pingPayload
    }

    @ApolloExperimental
    fun pongPayload(pongPayload: Map<String, Any?>?) {
      this.pongPayload = pongPayload
    }

    @ApolloExperimental
    fun connectionAcknowledgeTimeoutMillis(connectionAcknowledgeTimeoutMillis: Long) {
      this.connectionAcknowledgeTimeoutMs = connectionAcknowledgeTimeoutMillis
    }

    @ApolloExperimental
    fun frameType(frameType: WsFrameType) {
      this.frameType = frameType
    }

    @ApolloExperimental
    fun webSocketPayloadComposer(webSocketPayloadComposer: WebSocketPayloadComposer) {
      this.webSocketPayloadComposer = webSocketPayloadComposer
    }

    override val name: String
      get() = "graphql-transport-ws"

    override fun create(webSocketConnection: WebSocketConnection, listener: Listener, scope: CoroutineScope): WsProtocol {
      val connectionPayload = connectionPayload ?: { null }
      val connectionAcknowledgeTimeoutMs = connectionAcknowledgeTimeoutMs ?: 10_000
      val pingIntervalMillis = pingIntervalMillis ?: -1
      val frameType = frameType ?: WsFrameType.Text

      @Suppress("DEPRECATION")
      return GraphQLWsProtocol(
          connectionPayload = connectionPayload,
          pingPayload = pingPayload,
          pongPayload = pongPayload,
          connectionAcknowledgeTimeoutMs = connectionAcknowledgeTimeoutMs,
          pingIntervalMillis = pingIntervalMillis,
          frameType = frameType,
          webSocketConnection = webSocketConnection,
          listener = listener,
          scope = scope,
          webSocketPayloadComposer = webSocketPayloadComposer ?: DefaultWebSocketPayloadComposer()
      )
    }
  }
}
