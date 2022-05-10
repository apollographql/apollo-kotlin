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
import okio.ByteString.Companion.encodeUtf8
import kotlin.test.Test

@OptIn(ApolloExperimental::class)
class CustomHandlerTest {
  private lateinit var mockServer: MockServer

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun customHandler() = runTest(after = { tearDown() }) {
    val body0 = "Hello, World! 000"
    val mockResponse0 = MockResponse(
        body = body0,
        statusCode = 404,
        headers = mapOf("Content-Type" to "text/plain"),
    )
    val body1 = "Hello, World! 001"
    val mockResponse1 = MockResponse(
        body = body1,
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
    }

    mockServer = MockServer(mockServerHandler)

    val engine = DefaultHttpEngine()

    var httpResponse = engine.execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url() + "1").build())
    assertMockResponse(mockResponse = mockResponse1, body = body1.encodeUtf8(), httpResponse = httpResponse)

    httpResponse = engine.execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url() + "0").build())
    assertMockResponse(mockResponse = mockResponse0, body = body0.encodeUtf8(), httpResponse = httpResponse)

    httpResponse = engine.execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url() + "1").build())
    assertMockResponse(mockResponse = mockResponse1, body = body1.encodeUtf8(), httpResponse = httpResponse)
  }

}
