package com.apollographql.apollo3.mockserver

import com.apollographql.apollo3.annotations.ApolloExperimental

interface MockServerInterface {
  /**
   * Returns the root url for this server
   *
   * It will suspend until a port is found to listen to
   */
  suspend fun url(): String

  /**
   * Stops the server
   */
  suspend fun stop()

  /**
   * The mock server handler used to respond to requests.
   *
   * The default handler is a [QueueMockServerHandler], which serves a fixed sequence of responses from a queue (see [enqueue]).
   */
  val mockServerHandler: MockServerHandler

  /**
   * Enqueue a response
   */
  fun enqueue(mockResponse: MockResponse)

  /**
   * Returns a request from the recorded requests or throws if no request has been received
   */
  fun takeRequest(): MockRequest
}

@ApolloExperimental
expect class MockServer(mockServerHandler: MockServerHandler = QueueMockServerHandler()) : MockServerInterface {
  override suspend fun url(): String
  override suspend fun stop()
  override val mockServerHandler: MockServerHandler
  override fun enqueue(mockResponse: MockResponse)
  override fun takeRequest(): MockRequest
}

@ApolloExperimental
fun MockServer.enqueue(string: String = "", delayMs: Long = 0, statusCode: Int = 200) {
  enqueue(MockResponse.Builder()
      .statusCode(statusCode)
      .body(string)
      .delayMillis(delayMs)
      .build())
}