package com.apollographql.apollo3.mockserver

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8

class MockResponse
@Deprecated("Use MockResponse.Builder instead", ReplaceWith("MockResponse.Builder().statusCode(statusCode).headers(headers).body(body).delayMillis(delayMillis).build()"), level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_3_1)
constructor(
    val statusCode: Int = 200,
    val body: Flow<ByteString> = emptyFlow(),
    val headers: Map<String, String> = mapOf("Content-Length" to "0"),
    val delayMillis: Long = 0,
) {
  fun newBuilder(): Builder {
    return Builder()
        .statusCode(statusCode)
        .headers(headers)
        .delayMillis(delayMillis)
        .body(body)
  }
  class Builder {
    private var statusCode: Int = 200
    private var body: Flow<ByteString> = emptyFlow()
    private val headers = mutableMapOf<String, String>()
    private var delayMillis: Long = 0
    private var contentLength: Int? = null

    fun statusCode(statusCode: Int) = apply { this.statusCode = statusCode }

    fun body(body: Flow<ByteString>) = apply { this.body = body }

    fun body(body: ByteString) = apply {
      this.body = flowOf(body)
      contentLength = body.size
    }

    fun body(body: String) = body(body.encodeUtf8())

    fun headers(headers: Map<String, String>) = apply {
      this.headers.clear()
      this.headers += headers
    }

    fun addHeader(key: String, value: String) = apply { headers[key] = value }

    fun delayMillis(delayMillis: Long) = apply { this.delayMillis = delayMillis }

    fun build(): MockResponse {
      val headersWithContentLength = buildMap {
        putAll(headers)
        if (contentLength != null) {
          put("Content-Length", contentLength.toString())
        }
      }

      // https://youtrack.jetbrains.com/issue/KT-34480
      @Suppress("DEPRECATION_ERROR")
      return MockResponse(statusCode = statusCode, body = body, headers = headersWithContentLength, delayMillis = delayMillis)
    }
  }
}