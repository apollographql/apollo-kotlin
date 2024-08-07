package com.apollographql.apollo.network.websocket

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.json.readAny
import okio.Buffer

/**
 * An [WsProtocol] for https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md
 *
 * [GraphQLWsProtocol] can execute queries and mutations in addition to subscriptions
 */
@ApolloExperimental
class GraphQLWsProtocol(
    val connectionPayload: suspend () -> Any? = { null },
) : WsProtocol {
  override val name: String
    get() = "graphql-transport-ws"

  override suspend fun connectionInit(): ClientMessage {
    val map = mutableMapOf<String, Any?>()
    map.put("type", "connection_init")
    val payload = connectionPayload()
    if (payload != null) {
      map.put("payload", payload)
    }

    return map.toClientMessage()
  }

  override suspend fun <D : Operation.Data> operationStart(request: ApolloRequest<D>): ClientMessage {
    return mapOf(
        "type" to "subscribe",
        "id" to request.requestUuid.toString(),
        "payload" to DefaultHttpRequestComposer.composePayload(request)
    ).toClientMessage()
  }

  override fun <D : Operation.Data> operationStop(request: ApolloRequest<D>): ClientMessage {
    return mapOf(
        "type" to "complete",
        "id" to request.requestUuid.toString(),
    ).toClientMessage()
  }

  override fun ping(): ClientMessage {
    return mapOf("type" to "ping").toClientMessage()
  }

  override fun pong(): ClientMessage {
    return mapOf("type" to "pong").toClientMessage()
  }

  override fun parseServerMessage(text: String): ServerMessage {
    val map = try {
      @Suppress("UNCHECKED_CAST")
      Buffer().writeUtf8(text).jsonReader().readAny() as Map<String, Any?>
    } catch (e: Exception) {
      return ParseErrorServerMessage("Invalid JSON: '$text'")
    }

    val type = map["type"] as? String
    if (type == null) {
      return ParseErrorServerMessage("No 'type' found in server message: '$text'")
    }

    return when (type) {
      "connection_ack" -> ConnectionAckServerMessage
      "ping" -> PingServerMessage
      "pong" -> PongServerMessage
      "next", "complete", "error" -> {
        val id = map["id"] as? String
        when {
          id == null -> ParseErrorServerMessage("No 'id' found in message: '$text'")
          type == "next" -> ResponseServerMessage(id, map["payload"])
          type == "complete" -> CompleteServerMessage(id)
          type == "error" -> OperationErrorServerMessage(id, map["payload"])
          else -> error("") // make the compiler happy
        }
      }

      else -> ParseErrorServerMessage("Unknown type: '$type' found in server message: '$text'")
    }
  }
}