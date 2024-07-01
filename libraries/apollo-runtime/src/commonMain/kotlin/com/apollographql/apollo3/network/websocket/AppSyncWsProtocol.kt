package com.apollographql.apollo.network.websocket

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.NullableAnyAdapter
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo.api.http.DefaultHttpRequestComposer.Companion.appendQueryParameters
import com.apollographql.apollo.api.json.buildJsonByteString
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.json.readAny
import com.apollographql.apollo.api.json.writeAny
import com.apollographql.apollo.api.toJsonString
import okio.Buffer

/**
 * A [WsProtocol] for https://docs.aws.amazon.com/appsync/latest/devguide/real-time-websocket-client.html
 */
@ApolloExperimental
class AppSyncWsProtocol(
    val authorization: suspend () -> Any? = { null },
) : WsProtocol {
  override val name: String
    get() = "graphql-ws"

  override suspend fun connectionInit(): ClientMessage {
    return mapOf("type" to "connection_init").toClientMessage()
  }

  override suspend fun <D : Operation.Data> operationStart(request: ApolloRequest<D>): ClientMessage {
    // AppSync encodes the data as a String
    val data = NullableAnyAdapter.toJsonString(DefaultHttpRequestComposer.composePayload(request))


        return mapOf(
            "type" to "start",
            "id" to request.requestUuid.toString(),
            "payload" to mapOf(
                "data" to data,
                "extensions" to mapOf(
                    "authorization" to authorization()
                )
            )
        ).toClientMessage()
  }

  override fun <D : Operation.Data> operationStop(request: ApolloRequest<D>): ClientMessage {
    return mapOf(
        "type" to "stop",
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
      return ParseErrorServerMessage("Invalid JSON: '$this'")
    }

    val type = map["type"] as? String
    if (type == null) {
      return ParseErrorServerMessage("No 'type' found in server message: '$this'")
    }

    return when (type) {
      "connection_ack" -> ConnectionAckServerMessage
      "connection_error" -> ConnectionErrorServerMessage(map["payload"])
      "ka" -> PingServerMessage
      "data", "complete" -> {
        val id = map["id"] as? String
        when {
          id == null -> ParseErrorServerMessage("No 'id' found in message: '$text'")
          type == "data" -> ResponseServerMessage(id, map["payload"])
          type == "complete" -> CompleteServerMessage(id)
          else -> error("") // make the compiler happy
        }
      }
      "error" -> {
        val id = map["id"] as? String
        if (id != null) {
          OperationErrorServerMessage(id, map["payload"])
        } else {
          ParseErrorServerMessage("General error: $text")
        }
      }

      else -> ParseErrorServerMessage("Unknown type: '$type' found in server message: '$text'")
    }
  }

  @ApolloExperimental
  companion object {
    /**
     * Helper method that builds the final URL. It will append the authorization and payload arguments as query parameters.
     * This method can be used for both the HTTP URL as well as the WebSocket URL
     *
     * Example:
     * ```
     * buildUrl(
     *   baseUrl = "https://example1234567890000.appsync-realtime-api.us-east-1.amazonaws.com/graphql",
     *   // This example uses an API key. See the AppSync documentation for information on what to pass
     *   authorization = mapOf(
     *     "host" to "example1234567890000.appsync-api.us-east-1.amazonaws.com",
     *     "x-api-key" to "da2-12345678901234567890123456"
     *   )
     * )
     * ```
     *
     * @param baseUrl The base web socket URL.
     * @param authorization The authorization as per the AppSync documentation.
     * @param payload An optional payload. Defaults to an empty object.
     */
    fun buildUrl(
        baseUrl: String,
        authorization: Map<String, Any?>,
        payload: Map<String, Any?> = emptyMap(),
    ): String =
        baseUrl
            .appendQueryParameters(mapOf(
                "header" to authorization.base64Encode(),
                "payload" to payload.base64Encode(),
            ))

    private fun Map<String, Any?>.base64Encode(): String {
      return buildJsonByteString {
        writeAny(this@base64Encode)
      }.base64()
    }
  }
}