package com.apollographql.apollo3.mockserver.test

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.mockserver.MockDispatcher
import com.apollographql.apollo3.mockserver.MockRecordedRequest
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.readRequest
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.testing.runTest
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ApolloExperimental::class)
class MockServerCommonTest {
  private lateinit var mockServer: MockServer

  private fun setUp() {
    mockServer = MockServer()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun readGetRequest() {
    val request = """
      GET / HTTP/2
      Host: github.com
      User-Agent: curl/7.64.1
      Accept: */*
    """.trimIndent()
        .split("\n")
        .joinToString(separator = "\r\n", postfix = "\r\n\r\n")

    val recordedRequest = readRequest(Buffer().apply { writeUtf8(request) })
    assertNotNull(recordedRequest)
    assertEquals("GET", recordedRequest.method)
    assertEquals("/", recordedRequest.path)
    assertEquals("HTTP/2", recordedRequest.version)
    assertEquals(mapOf(
        "Host" to "github.com",
        "User-Agent" to "curl/7.64.1",
        "Accept" to "*/*"
    ), recordedRequest.headers)
    assertEquals(0, recordedRequest.body.size)
  }

  @Test
  fun readPostRequest() {
    val request = """
      POST / HTTP/2
      Content-Length: 11
      
      Hello world
    """.trimIndent()
        .split("\n")
        .joinToString(separator = "\r\n", postfix = "")

    val recordedRequest = readRequest(Buffer().apply { writeUtf8(request) })
    assertNotNull(recordedRequest)
    assertEquals("POST", recordedRequest.method)
    assertEquals("Hello world", recordedRequest.body.utf8())
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
  fun exceptionWhenNothingWasEnqueued() = runTest(before = { setUp() }, after = { tearDown() }) {
    val engine = DefaultHttpEngine(timeoutMillis = 10)
    assertFailsWith<ApolloNetworkException> {
      engine.execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url()).build())
    }
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

  private fun assertMockResponse(
      mockResponse: MockResponse,
      httpResponse: HttpResponse,
  ) {
    assertEquals(mockResponse.body.utf8(), httpResponse.body!!.readUtf8())
    assertEquals(mockResponse.statusCode, httpResponse.statusCode)
    // JS MockServer serves headers in lowercase, so convert before comparing
    val mockResponseHeaders = mockResponse.headers.map { it.key.lowercase() to it.value.lowercase() }
    val httpResponseHeaders = httpResponse.headers.map { it.name.lowercase() to it.value.lowercase() }

    // Use contains because there can be more headers than requested in the response
    assertTrue(httpResponseHeaders.containsAll(mockResponseHeaders), "Headers do not match: httpResponseHeaders=$httpResponseHeaders mockResponseHeadersLowercase=$mockResponseHeaders")
  }

}
