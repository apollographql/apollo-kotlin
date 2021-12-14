package com.apollographql.apollo3.network.ws

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.NullableAnyAdapter
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer.Companion.appendQueryParameters
import com.apollographql.apollo3.api.json.buildJsonByteString
import com.apollographql.apollo3.api.json.writeAny
import com.apollographql.apollo3.api.toJsonString
import com.apollographql.apollo3.exception.ApolloNetworkException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeout

/**
 * A [WsProtocol] for https://docs.aws.amazon.com/appsync/latest/devguide/real-time-websocket-client.html
 */
class AppSyncWsProtocol(
    private val authorization: Map<String, Any?>,
    private val connectionAcknowledgeTimeoutMs: Long,
    webSocketConnection: WebSocketConnection,
    listener: Listener,
) : WsProtocol(webSocketConnection, listener) {

  override suspend fun connectionInit() {
    val message = mutableMapOf<String, Any?>(
        "type" to "connection_init",
    )

    sendMessageMapText(message)

    withTimeout(connectionAcknowledgeTimeoutMs) {
      val map = receiveMessageMap()
      when (val type = map["type"]) {
        "connection_ack" -> return@withTimeout
        "connection_error" -> throw ApolloNetworkException("Connection error:\n$map")
        else -> println("unknown AppSync message while waiting for connection_ack: '$type")
      }
    }
  }

  override fun <D : Operation.Data> startOperation(request: ApolloRequest<D>) {
    // AppSync encodes the data as a String
    val data = NullableAnyAdapter.toJsonString(DefaultHttpRequestComposer.composePayload(request), CustomScalarAdapters.Empty, )

    sendMessageMapText(
        mapOf(
            "type" to "start",
            "id" to request.requestUuid.toString(),
            "payload" to mapOf(
                "data" to data,
                "extensions" to mapOf(
                    "authorization" to authorization
                )
            )
        )
    )
  }

  override fun <D : Operation.Data> stopOperation(request: ApolloRequest<D>) {
    sendMessageMapText(
        mapOf(
            "type" to "stop",
            "id" to request.requestUuid.toString()
        )
    )
  }

  override fun handleServerMessage(messageMap: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    return when (messageMap["type"]) {
      "data" -> listener.operationResponse(messageMap["id"] as String, messageMap["payload"] as Map<String, Any?>)
      "error" -> {
        val id = messageMap["id"]
        if (id is String) {
          listener.operationError(id, messageMap["payload"] as Map<String, Any?>?)
        } else {
          listener.generalError(messageMap["payload"] as Map<String, Any?>?)
        }
      }
      "complete" -> listener.operationComplete(messageMap["id"] as String)
      "ka" -> Unit // Keep Alive: nothing to do
      else -> Unit // Unknown message
    }
  }

  /**
   * @param authorization: a map containing the authorization information. For an example:
   * ```
   *  mapOf(
   *     "host" to "example1234567890000.appsync-api.us-east-1.amazonaws.com",
   *     "x-api-key" to "da2-12345678901234567890123456"
   *   )
   * ```
   *
   */
  class Factory(
      private val authorization: Map<String, Any?>,
      private val connectionAcknowledgeTimeoutMs: Long = 10_000,
  ) : WsProtocol.Factory {
    override val name: String
      get() = "graphql-ws"

    override fun create(
        webSocketConnection: WebSocketConnection,
        listener: Listener,
        scope: CoroutineScope
    ): WsProtocol {
      return AppSyncWsProtocol(
          authorization = authorization,
          webSocketConnection = webSocketConnection,
          connectionAcknowledgeTimeoutMs = connectionAcknowledgeTimeoutMs,
          listener = listener
      )
    }
  }

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
      @OptIn(ApolloInternal::class)
      return buildJsonByteString {
        writeAny(this@base64Encode)
      }.base64()
    }
  }
}