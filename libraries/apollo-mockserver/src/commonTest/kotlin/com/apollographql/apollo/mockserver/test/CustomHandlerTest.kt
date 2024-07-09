@file:Suppress("DEPRECATION")

package com.apollographql.apollo.mockserver.test

import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.mockserver.MockRequestBase
import com.apollographql.apollo.mockserver.MockResponse
import com.apollographql.apollo.mockserver.MockServer
import com.apollographql.apollo.mockserver.MockServerHandler
import com.apollographql.apollo.network.http.DefaultHttpEngine
import com.apollographql.apollo.testing.internal.runTest
import kotlin.test.Test

class CustomHandlerTest {
  private lateinit var mockServer: MockServer

  private suspend fun tearDown() {
    mockServer.close()
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
      override fun handle(request: MockRequestBase): MockResponse {
        return when (request.path) {
          "/0" -> mockResponse0
          "/1" -> mockResponse1
          else -> error("Unexpected path: ${request.path}")
        }
      }
    }

    mockServer = MockServer.Builder().handler(mockServerHandler).build()

    val engine = DefaultHttpEngine()

    var httpResponse = engine.execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url() + "1").build())
    assertMockResponse(mockResponse1, httpResponse)

    httpResponse = engine.execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url() + "0").build())
    assertMockResponse(mockResponse0, httpResponse)

    httpResponse = engine.execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url() + "1").build())
    assertMockResponse(mockResponse1, httpResponse)
  }

}
