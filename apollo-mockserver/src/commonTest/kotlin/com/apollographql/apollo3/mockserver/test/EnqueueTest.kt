package com.apollographql.apollo3.mockserver.test

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.testing.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    val mockResponses = listOf(
        MockResponse(
            body = "Hello, World! 000",
            statusCode = 404,
            headers = mapOf("Content-Type" to "text/plain"),
        ),
        MockResponse(
            body = "Hello, World! 001",
            statusCode = 200,
            headers = mapOf("X-Test" to "true"),
        ),
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

  @Test
  fun status500WhenNothingWasEnqueued() = runTest(before = { setUp() }, after = { tearDown() }) {
    val engine = DefaultHttpEngine()
    val httpResponse = engine.execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url()).build())
    assertEquals(500, httpResponse.statusCode)
    assertTrue(httpResponse.body!!.readUtf8().contains("No more responses in queue"))
  }
}
