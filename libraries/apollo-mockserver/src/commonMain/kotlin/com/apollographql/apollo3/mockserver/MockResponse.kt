package com.apollographql.apollo3.mockserver

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8

class MockResponse private constructor(
    val statusCode: Int,
    val body: Flow<ByteString>,
    val headers: Map<String, String>,
    val delayMillis: Long,
    /**
     * Whether to keep the TCP connection alive after this response
     */
    val keepAlive: Boolean
) {

  @Deprecated("Use MockResponse.Builder instead", ReplaceWith("MockResponse.Builder().statusCode(statusCode).headers(headers).body(body).delayMillis(delayMillis).build()"), level = DeprecationLevel.ERROR)
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_3_1)
  constructor(
      statusCode: Int = 200,
      body: Flow<ByteString> = emptyFlow(),
      headers: Map<String, String> = mapOf("Content-Length" to "0"),
      delayMillis: Long = 0,
  ): this(statusCode, body, headers, delayMillis, true)
  fun newBuilder(): Builder {
    return Builder()
        .statusCode(statusCode)
        .headers(headers)
        .delayMillis(delayMillis)
        .body(body)
        .keepAlive(keepAlive)
  }
  class Builder {
    private var statusCode: Int = 200
    private var body: Flow<ByteString> = emptyFlow()
    private val headers = mutableMapOf<String, String>()
    private var delayMillis: Long = 0
    private var contentLength: Int? = null
    private var keepAlive = true

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

    fun keepAlive(keepAlive: Boolean) = apply {
      this.keepAlive = keepAlive
    }
    fun build(): MockResponse {
      val headersWithContentLength = buildMap {
        putAll(headers)
        if (contentLength != null) {
          put("Content-Length", contentLength.toString())
        }
      }

      return MockResponse(statusCode = statusCode, body = body, headers = headersWithContentLength, delayMillis = delayMillis, keepAlive = keepAlive)
    }
  }
}

