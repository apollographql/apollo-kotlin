package com.apollographql.apollo3.mockserver.test

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.mockserver.MockDispatcher
import com.apollographql.apollo3.mockserver.MockRecordedRequest
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.testing.runTest
import kotlin.test.Test

@OptIn(ApolloExperimental::class)
class CustomDispatcherTest {
  private lateinit var mockServer: MockServer

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun dispatch() = runTest(after = { tearDown() }) {
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

    val mockDispatcher = object : MockDispatcher {
      override fun dispatch(request: MockRecordedRequest): MockResponse {
        return when (request.path) {
          "/0" -> mockResponse0
          "/1" -> mockResponse1
          else -> error("Unexpected path: ${request.path}")
        }
      }

      override fun copy() = this
    }

    mockServer = MockServer(mockDispatcher)

    val engine = DefaultHttpEngine()

    var httpResponse = engine.execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url() + "1").build())
    assertMockResponse(mockResponse1, httpResponse)

    httpResponse = engine.execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url() + "0").build())
    assertMockResponse(mockResponse0, httpResponse)

    httpResponse = engine.execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url() + "1").build())
    assertMockResponse(mockResponse1, httpResponse)
  }

}
