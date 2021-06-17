package com.apollographql.apollo3.subscription

import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.internal.json.Utils
import okhttp3.HttpUrl
import okio.Buffer

/**
 * TODO: move to commonMain.
 * We're missing OkHttp HttpUrl
 */
object AppSync {

  /**
   * Helper method that builds the final web socket URL. It will append the authorization and payload arguments as query parameters.
   *
   * Example:
   * ```
   * buildWebSocketUrl(
   *   baseWebSocketUrl = "wss://example1234567890000.appsync-realtime-api.us-east-1.amazonaws.com/graphql",
   *   // This example uses an API key. See the AppSync documentation for information on what to pass
   *   authorization = mapOf(
   *     "host" to "example1234567890000.appsync-api.us-east-1.amazonaws.com",
   *     "x-api-key" to "da2-12345678901234567890123456"
   *   )
   * )
   * ```
   *
   * @param baseWebSocketUrl The base web socket URL.
   * @param authorization The authorization as per the AppSync documentation.
   * @param payload An optional payload. Defaults to an empty object.
   */
  fun buildWebSocketUrl(
      baseWebSocketUrl: String,
      authorization: Map<String, Any?>,
      payload: Map<String, Any?> = emptyMap(),
  ): String =
      baseWebSocketUrl
          .let { url ->
            when {
              url.startsWith("ws://", ignoreCase = true) -> "http" + url.drop(2)
              url.startsWith("wss://", ignoreCase = true) -> "https" + url.drop(3)
              else -> url
            }
          }
          .let { HttpUrl.get(it) }
          .newBuilder()
          .setQueryParameter("header", authorization.base64Encode())
          .setQueryParameter("payload", payload.base64Encode())
          .build()
          .toString()
          .let { url ->
            when {
              url.startsWith("http://", ignoreCase = true) -> "ws" + url.drop(4)
              url.startsWith("https://", ignoreCase = true) -> "wss" + url.drop(5)
              else -> url
            }
          }

  private fun Map<String, Any?>.base64Encode(): String {
    val buffer = Buffer()
    Utils.writeToJson(this, BufferedSinkJsonWriter(buffer))
    return buffer.readByteString().base64()
  }
}