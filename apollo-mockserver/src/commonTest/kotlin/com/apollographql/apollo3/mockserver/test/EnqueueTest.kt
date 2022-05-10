package com.apollographql.apollo3.mockserver.test

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.asChunked
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.testing.runTest
import kotlinx.coroutines.flow.flowOf
import okio.ByteString.Companion.encodeUtf8
import kotlin.test.Test

@OptIn(ApolloExperimental::class)
class EnqueueTest {
  private lateinit var mockServer: MockServer

  private fun setUp() {
    mockServer = MockServer()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun enqueue() = runTest(before = { setUp() }, after = { tearDown() }) {
    val body0 = "Hello, World! 000"
    val body1 = "Hello, World! 001"
    val body2 = "First chunk\nSecond chunk"
    val mockResponses = mapOf(
        MockResponse(
            body = body0,
            statusCode = 404,
            headers = mapOf("Content-Type" to "text/plain"),
        ) to body0,
        MockResponse(
            body = body1,
            statusCode = 200,
            headers = mapOf("X-Test" to "true"),
        ) to body1,
        MockResponse(
            body = flowOf("First chunk\n".encodeUtf8(), "Second chunk".encodeUtf8()).asChunked(),
            statusCode = 200,
            headers = mapOf("X-Test" to "false", "Transfer-Encoding" to "chunked"),
        ) to body2,
    )
    for (mockResponse in mockResponses) {
      mockServer.enqueue(mockResponse.key)
    }

    val engine = DefaultHttpEngine()
    for (mockResponse in mockResponses) {
      val httpResponse = engine.execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url()).build())
      assertMockResponse(mockResponse = mockResponse.key, body = mockResponse.value.encodeUtf8(), httpResponse = httpResponse)
    }
  }
}
