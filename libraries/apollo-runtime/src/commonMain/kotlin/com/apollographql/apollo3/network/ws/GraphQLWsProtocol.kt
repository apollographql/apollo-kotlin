package com.apollographql.apollo3.network.ws

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_2_3
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout


/**
 * An [WsProtocol] that uses https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md
 * It can carry queries in addition to subscriptions over the websocket
 */
class GraphQLWsProtocol(
    private val connectionPayload: suspend () -> Map<String, Any?>? = { null },
    private val pingPayload: Map<String, Any?>? = null,
    private val pongPayload: Map<String, Any?>? = null,
    private val connectionAcknowledgeTimeoutMs: Long,
    private val pingIntervalMillis: Long,
    private val frameType: WsFrameType,
    webSocketConnection: WebSocketConnection,
    listener: Listener,
    private val scope: CoroutineScope,
) : WsProtocol(webSocketConnection, listener) {

  @Deprecated("Use the constructor with connectionPayload as a lambda instead",
      ReplaceWith("Factory({ connectionPayload }, pingIntervalMillis, pingPayload, pongPayload, connectionAcknowledgeTimeoutMs)"))
  @ApolloDeprecatedSince(v3_2_3)
  constructor(
      connectionPayload: Map<String, Any?>? = null,
      pingPayload: Map<String, Any?>? = null,
      pongPayload: Map<String, Any?>? = null,
      connectionAcknowledgeTimeoutMs: Long,
      pingIntervalMillis: Long,
      frameType: WsFrameType,
      webSocketConnection: WebSocketConnection,
      listener: Listener,
      scope: CoroutineScope,
  ) : this(
      connectionPayload = { connectionPayload },
      pingPayload = pingPayload,
      pongPayload = pongPayload,
      connectionAcknowledgeTimeoutMs = connectionAcknowledgeTimeoutMs,
      pingIntervalMillis = pingIntervalMillis,
      frameType = frameType,
      webSocketConnection = webSocketConnection,
      listener = listener,
      scope = scope,
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
        else -> println("unknown graphql-ws message while waiting for connection_ack: '$type")
      }
    }
  }

  override fun <D : Operation.Data> startOperation(request: ApolloRequest<D>) {
    sendMessageMap(
        mapOf(
            "type" to "subscribe",
            "id" to request.requestUuid.toString(),
            "payload" to DefaultHttpRequestComposer.composePayload(request)
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
      "error" -> listener.operationError(messageMap["id"] as String, messageMap["payload"] as Map<String, Any?>)
      "complete" -> listener.operationComplete(messageMap["id"] as String)
      "ping" -> {
        val map = mutableMapOf<String, Any?>(
            "type" to "pong",
        )
        if (pongPayload != null) {
          map["payload"] = pongPayload
        }
        sendMessageMap(map, frameType)
      }
      "pong" -> Unit // Nothing to do, the server acknowledged one of our pings
      else -> Unit // Unknown message
    }
  }

  /**
   * A factory for [GraphQLWsProtocol].
   *
   * @param connectionPayload a map of additional parameters to send in the "connection_init" message
   * @param pingIntervalMillis the interval between two client-initiated pings or -1 to not send any ping.
   * Default value: -1
   * @param pingPayload the ping payload to send in "ping" messages or null to not send a payload
   * @param pongPayload the pong payload to send in "pong" messages or null to not send a payload
   * @param connectionAcknowledgeTimeoutMs the timeout for receiving the "connection_ack" message, in milliseconds
   * @param frameType the type of the websocket frames to use. Default value: [WsFrameType.Text]
   */
  class Factory(
      private val connectionPayload: suspend () -> Map<String, Any?>? = { null },
      private val pingIntervalMillis: Long = -1,
      private val pingPayload: Map<String, Any?>? = null,
      private val pongPayload: Map<String, Any?>? = null,
      private val connectionAcknowledgeTimeoutMs: Long = 10_000,
      private val frameType: WsFrameType = WsFrameType.Text,
  ) : WsProtocol.Factory {

    @Deprecated("Use the constructor with connectionPayload as a lambda instead",
        ReplaceWith("Factory({ connectionPayload }, pingIntervalMillis, pingPayload, pongPayload, connectionAcknowledgeTimeoutMs)"))
    @ApolloDeprecatedSince(v3_2_3)
    constructor(
        connectionPayload: Map<String, Any?>?,
        pingIntervalMillis: Long = -1,
        pingPayload: Map<String, Any?>? = null,
        pongPayload: Map<String, Any?>? = null,
        connectionAcknowledgeTimeoutMs: Long = 10_000,
        frameType: WsFrameType = WsFrameType.Text,
    ) : this(
        connectionPayload = { connectionPayload },
        pingIntervalMillis = pingIntervalMillis,
        pingPayload = pingPayload,
        pongPayload = pongPayload,
        connectionAcknowledgeTimeoutMs = connectionAcknowledgeTimeoutMs,
        frameType = frameType,
    )

    override val name: String
      get() = "graphql-transport-ws"

    override fun create(webSocketConnection: WebSocketConnection, listener: Listener, scope: CoroutineScope): WsProtocol {
      return GraphQLWsProtocol(
          connectionPayload = connectionPayload,
          pingPayload = pingPayload,
          pongPayload = pongPayload,
          connectionAcknowledgeTimeoutMs = connectionAcknowledgeTimeoutMs,
          pingIntervalMillis = pingIntervalMillis,
          frameType = frameType,
          webSocketConnection = webSocketConnection,
          listener = listener,
          scope = scope
      )
    }
  }
}
