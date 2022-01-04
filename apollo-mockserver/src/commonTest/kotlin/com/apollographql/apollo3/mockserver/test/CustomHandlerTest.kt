package com.apollographql.apollo3.mockserver.test

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.mockserver.MockRequest
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.MockServerHandler
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.testing.runTest
import kotlin.test.Test

@OptIn(ApolloExperimental::class)
class CustomHandlerTest {
  private lateinit var mockServer: MockServer

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun customHandler() = runTest(after = { tearDown() }) {
    val mockResponse0 = MockResponse(
        body = "Hello, World! 000",
        statusCode = 404,
        headers = mapOf("Content-Type" to "text/plain"),
    )
    val mockResponse1 = MockResponse(
        body = "Hello, World! 001",
        statusCode = 200,
        headers = mapOf("X-Test" to "true"),
    )

    val mockServerHandler = object : MockServerHandler {
      override fun handle(request: MockRequest): MockResponse {
        return when (request.path) {
          "/0" -> mockResponse0
          "/1" -> mockResponse1
          else -> error("Unexpected path: ${request.path}")
        }
      }

      override fun copy() = this
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
