package com.apollographql.apollo3.mockserver

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8

class MockResponse internal constructor(
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
      val headersWithContentLength = if (contentLength == null) headers else headers + mapOf("Content-Length" to contentLength.toString())
      return MockResponse(statusCode = statusCode, body = body, headers = headersWithContentLength, delayMillis = delayMillis)
    }
  }
}