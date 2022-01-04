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
   * The dispatcher used to respond to requests.
   *
   * The default dispatcher is a QueueMockDispatcher, which serves a fixed sequence of responses from a queue (see [enqueue]).
   */
  val mockDispatcher: MockDispatcher

  /**
   * Enqueue a response
   */
  fun enqueue(mockResponse: MockResponse)

  /**
   * Returns a request from the recorded requests or throws if no request has been received
   */
  fun takeRequest(): MockRecordedRequest
}

abstract class BaseMockServer(override val mockDispatcher: MockDispatcher) : MockServerInterface {
  override fun enqueue(mockResponse: MockResponse) {
    (mockDispatcher as? QueueMockDispatcher)?.enqueue(mockResponse) ?: error("Apollo: cannot call MockServer.enqueue() with a custom dispatcher")
  }
}


@ApolloExperimental
expect class MockServer(mockDispatcher: MockDispatcher = QueueMockDispatcher()) : BaseMockServer
