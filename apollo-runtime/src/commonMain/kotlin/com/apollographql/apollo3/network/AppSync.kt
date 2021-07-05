package com.apollographql.apollo3.network

import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer.Companion.appendQueryParameters
import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.internal.json.Utils
import okio.Buffer

object AppSync {

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
    val buffer = Buffer()
    Utils.writeToJson(this, BufferedSinkJsonWriter(buffer))
    return buffer.readByteString().base64()
  }
}
