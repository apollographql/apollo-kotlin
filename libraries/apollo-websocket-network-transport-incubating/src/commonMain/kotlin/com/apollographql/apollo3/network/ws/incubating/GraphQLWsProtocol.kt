package com.apollographql.apollo3.network.ws.incubating

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.json.readAny
import okio.Buffer

class GraphQLWsProtocol(
    val connectionParams: suspend () -> Any?,
) : WsProtocol {
  override val name: String
    get() = "graphql-transport-ws"

  override suspend fun connectionInit(): ClientMessage {
    val map = mutableMapOf<String, Any?>()
    map.put("type", "connection_init")
    val params = connectionParams()
    if (params != null) {
      map.put("payload", params)
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

  override fun ping(): ClientMessage? {
    return mapOf("type" to "ping").toClientMessage()
  }

  override fun pong(): ClientMessage? {
    return mapOf("type" to "pong").toClientMessage()
  }

  override fun parseServerMessage(text: String): ServerMessage {
    val map = try {
      @Suppress("UNCHECKED_CAST")
      Buffer().writeUtf8(text).jsonReader().readAny() as Map<String, Any?>
    } catch (e: Exception) {
      return ParseErrorServerMessage("Cannot parse server message: '$text'")
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
          type == "next" -> ResponseServerMessage(id, map["payload"], false)
          type == "complete" -> CompleteServerMessage(id)
          type == "error" -> ResponseServerMessage(id, mapOf("errors" to map["payload"]), true)
          else -> error("") // make the compiler happy
        }
      }

      else -> ParseErrorServerMessage("Unknown type: '$type' found in server message: '$text'")
    }
  }
}