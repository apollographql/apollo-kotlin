package com.apollographql.apollo3.mockserver.test

import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.asChunked
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.testing.internal.runTest
import kotlinx.coroutines.flow.flowOf
import okio.ByteString.Companion.encodeUtf8
import kotlin.test.Test

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
    val mockResponses = listOf(
        MockResponse.Builder()
            .body("Hello, World! 000")
            .statusCode(404)
            .addHeader("Content-Type", "text/plain")
            .build(),
        MockResponse.Builder()
            .body("Hello, World! 001")
            .statusCode(200)
            .addHeader("X-Test", "true")
            .build(),
        MockResponse.Builder()
            .body("ä".trimIndent())
            .build(),
        MockResponse.Builder()
            .body(flowOf("First chunk\n".encodeUtf8(), "Second chunk".encodeUtf8()).asChunked())
            .statusCode(200)
            .addHeader("X-Test", "false")
            .addHeader("Transfer-Encoding", "chunked")
            .build(),
    )
    for (mockResponse in mockResponses) {
      mockServer.enqueue(mockResponse)
    }

    val engine = DefaultHttpEngine()
    for (mockResponse in mockResponses) {
      val httpResponse = engine.execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url()).build())
      assertMockResponse(mockResponse, httpResponse)
    }
  }
}
