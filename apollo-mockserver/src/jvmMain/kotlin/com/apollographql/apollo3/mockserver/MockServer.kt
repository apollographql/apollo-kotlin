package com.apollographql.apollo3.mockserver

import okhttp3.Headers
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import java.util.concurrent.TimeUnit

actual class MockServer actual constructor(mockServerHandler: MockServerHandler) : BaseMockServer(mockServerHandler) {
  private val mockWebServer = MockWebServer().apply { dispatcher = mockServerHandler.toOkHttpDispatcher() }

  override fun takeRequest(): MockRequest {
    return mockWebServer.takeRequest(10, TimeUnit.MILLISECONDS)?.toApolloMockRequest() ?: error("No recorded request")
  }

  private fun RecordedRequest.toApolloMockRequest() = MockRequest(
      method = method!!,
      path = path!!,
      version = parseRequestLine(requestLine).third,
      headers = headers.toMap(),
      body = body.peek().readByteString()
  )

  private fun MockResponse.toOkHttpMockResponse() = okhttp3.mockwebserver.MockResponse()
      .setResponseCode(statusCode)
      .apply {
        this@toOkHttpMockResponse.headers.forEach {
          addHeader(it.key, it.value)
        }
      }.setBody(Buffer().apply { write(body) })
      .setHeadersDelay(delayMillis, TimeUnit.MILLISECONDS)

  private fun MockServerHandler.toOkHttpDispatcher() = object : Dispatcher() {
    override fun dispatch(request: RecordedRequest): okhttp3.mockwebserver.MockResponse {
      return handle(request.toApolloMockRequest()).toOkHttpMockResponse()
    }

    override fun shutdown() {}
  }

  private fun Headers.toMap(): Map<String, String> = (0.until(size)).map {
    name(it) to get(name(it))!!
  }.toMap()

  override suspend fun url(): String {
    return mockWebServer.url("/").toString()
  }

  override suspend fun stop() {
    mockWebServer.shutdown()
  }
}

