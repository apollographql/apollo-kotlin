package com.apollographql.apollo3.mockserver

import okhttp3.Headers
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import java.util.concurrent.TimeUnit

actual class MockServer {
  private val mockWebServer = MockWebServer()

  actual fun enqueue(mockResponse: MockResponse) {
    mockWebServer.enqueue(
        okhttp3.mockwebserver.MockResponse()
            .setResponseCode(mockResponse.statusCode)
            .apply {
              mockResponse.headers.forEach {
                addHeader(it.key, it.value)
              }
            }.setBody(Buffer().apply { write(mockResponse.body) })
            .setHeadersDelay(mockResponse.delayMs, TimeUnit.MILLISECONDS)
    )
  }

  actual fun takeRequest(): MockRecordedRequest {
    return mockWebServer.takeRequest(10, TimeUnit.MILLISECONDS)?.let {
      MockRecordedRequest(
          method = it.method!!,
          path = it.path!!,
          version = parseRequestLine(it.requestLine).third,
          headers = it.headers.toMap(),
          body = it.body.readByteString()
      )
    } ?: error("No recorded request")
  }

  private fun Headers.toMap(): Map<String, String> = (0.until(size)).map {
    name(it) to get(name(it))!!
  }.toMap()

  actual fun url(): String {
    return mockWebServer.url("/").toString()
  }

  actual fun stop() {
    mockWebServer.shutdown()
  }
}