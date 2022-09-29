package com.apollographql.apollo3.mockserver.test

import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.mockserver.MockRequest
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.MockServerHandler
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.testing.internal.runTest
import kotlin.test.Test

class CustomHandlerTest {
  private lateinit var mockServer: MockServer

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun customHandler() = runTest(after = { tearDown() }) {
    val mockResponse0 = MockResponse.Builder()
        .body("Hello, World! 000")
        .statusCode(404)
        .addHeader("Content-Type", "text/plain")
        .build()
    val mockResponse1 = MockResponse.Builder()
        .body("Hello, World! 001")
        .statusCode(200)
        .addHeader("X-Test", "true")
        .build()

    val mockServerHandler = object : MockServerHandler {
      override fun handle(request: MockRequest): MockResponse {
        return when (request.path) {
          "/0" -> mockResponse0
          "/1" -> mockResponse1
          else -> error("Unexpected path: ${request.path}")
        }
      }
    }

    mockServer = MockServer(mockServerHandler)

    val engine = DefaultHttpEngine()

    var httpResponse = engine.execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url() + "1").build())
    assertMockResponse(mockResponse1, httpResponse)

    httpResponse = engine.execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url() + "0").build())
    assertMockResponse(mockResponse0, httpResponse)

    httpResponse = engine.execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url() + "1").build())
    assertMockResponse(mockResponse1, httpResponse)
  }

}
