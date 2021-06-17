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

  override fun connectionInit(): Map<String, Any?> {
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

  @Suppress("UNCHECKED_CAST")
  override fun parseMessage(string: String): WsMessage {
    val map = AnyAdapter.fromResponse(BufferedSourceJsonReader(Buffer().writeUtf8(string))) as Map<String, Any?>

    return when (map["type"]) {
      "connection_ack" -> WsMessage.ConnectionAck
      "next" -> WsMessage.OperationData(map["id"] as String, map["payload"] as Map<String, Any?>)
      "error" -> WsMessage.OperationError(map["id"] as String, map["payload"] as Map<String, Any?>)
      "complete" -> WsMessage.OperationComplete(map["id"] as String)
      else -> WsMessage.Unknown(map)
    }
  }
}