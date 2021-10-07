package com.apollographql.apollo3.network.ws

import com.apollographql.apollo3.api.AnyAdapter
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.api.internal.json.BufferedSourceJsonReader
import okio.Buffer


/**
 * A [WsProtocol] for https://github.com/apollographql/subscriptions-transport-ws/blob/master/PROTOCOL.md
 */
class SubscriptionWsProtocol(
    private val connectionPayload: suspend () -> Map<String, Any?>? = {null},
    override val frameType: WsFrameType = WsFrameType.Binary
): WsProtocol {
  override val name: String
    get() = "graphql-ws"

  override suspend fun connectionInit(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>(
        "type" to "connection_init",
    )

    val payload = connectionPayload()
    if (payload != null) {
      map.put("payload", payload)
    }
    return map
  }

  override fun connectionTerminate(): Map<String, Any?> {
    return mapOf(
        "type" to "connection_terminate"
    )
  }

  override fun <D : Operation.Data> operationStart(request: ApolloRequest<D>): Map<String, Any?> {
    return mapOf(
        "type" to "start",
        "id" to request.requestUuid.toString(),
        "payload" to DefaultHttpRequestComposer.composePayload(request)
    )
  }

  override fun <D : Operation.Data> operationStop(request: ApolloRequest<D>): Map<String, Any?> {
    return mapOf(
        "type" to "stop",
        "id" to request.requestUuid.toString()
    )
  }

  override fun ping(payload: Map<String, Any?>?): Map<String, Any?>? = null
  override fun pong(payload: Map<String, Any?>?): Map<String, Any?>? = null

  @Suppress("UNCHECKED_CAST")
  override fun parseMessage(message: String, webSocketConnection: WebSocketConnection): WsMessage {
    val map = AnyAdapter.fromJson(BufferedSourceJsonReader(Buffer().writeUtf8(message))) as Map<String, Any?>

    return when (map["type"]) {
      "connection_ack" -> WsMessage.ConnectionAck
      "connection_error" -> WsMessage.ConnectionError(map["payload"] as Map<String, Any?>?)
      "data" -> WsMessage.OperationData(map["id"] as String, map["payload"] as Map<String, Any?>)
      "error" -> WsMessage.OperationError(map["id"] as String, map["payload"] as Map<String, Any?>)
      "complete" -> WsMessage.OperationComplete(map["id"] as String)
      "ka" -> WsMessage.KeepAlive
      else -> WsMessage.Unknown(map)
    }
  }
}