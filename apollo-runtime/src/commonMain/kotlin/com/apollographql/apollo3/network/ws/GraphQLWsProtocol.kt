package com.apollographql.apollo3.network.ws

import com.apollographql.apollo3.api.AnyAdapter
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.api.internal.json.BufferedSourceJsonReader
import okio.Buffer


/**
 * An [WsProtocol] that uses https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md
 * It can carry queries in addition to subscriptions over the websocket
 *
 * @param connectionPayload a map of additional parameters to send in the "connection_init" message
 */
class GraphQLWsProtocol(
    private val connectionPayload: Map<String, Any?>? = null,
    override val frameType: WsFrameType = WsFrameType.Binary,
) : WsProtocol {
  override val name: String
    get() = "graphql-transport-ws"

  override suspend fun connectionInit(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>(
        "type" to "connection_init",
    )
    if (connectionPayload != null) {
      map.put("payload", connectionPayload)
    }
    return map
  }

  override fun connectionTerminate(): Map<String, Any?>? = null

  override fun <D : Operation.Data> operationStart(request: ApolloRequest<D>): Map<String, Any?> {
    return mapOf(
        "type" to "subscribe",
        "id" to request.requestUuid.toString(),
        "payload" to DefaultHttpRequestComposer.composePayload(request)
    )
  }

  override fun <D : Operation.Data> operationStop(request: ApolloRequest<D>): Map<String, Any?> {
    return mapOf(
        "type" to "complete",
        "id" to request.requestUuid.toString()
    )
  }

  override fun ping(payload: Map<String, Any?>?): Map<String, Any?>? {
    val map = mutableMapOf<String, Any?>(
        "type" to "ping",
    )
    if (payload != null) {
      map["payload"] = payload
    }
    return map
  }

  override fun pong(payload: Map<String, Any?>?): Map<String, Any?>? {
    val map = mutableMapOf<String, Any?>(
        "type" to "pong",
    )
    if (payload != null) {
      map["payload"] = payload
    }
    return map
  }

  @Suppress("UNCHECKED_CAST")
  override fun parseMessage(message: String, webSocketConnection: WebSocketConnection): WsServerMessage {
    val map = AnyAdapter.fromJson(BufferedSourceJsonReader(Buffer().writeUtf8(message))) as Map<String, Any?>

    return when (map["type"]) {
      "connection_ack" -> WsServerMessage.ConnectionAck
      "next" -> WsServerMessage.OperationData(map["id"] as String, map["payload"] as Map<String, Any?>)
      "error" -> WsServerMessage.OperationError(map["id"] as String, map["payload"] as Map<String, Any?>)
      "complete" -> WsServerMessage.OperationComplete(map["id"] as String)
      "ping" -> WsServerMessage.Ping(map["payload"] as Map<String, Any?>?)
      "pong" -> WsServerMessage.Pong(map["payload"] as Map<String, Any?>?)
      else -> WsServerMessage.Unknown(map)
    }
  }
}