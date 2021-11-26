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
   * Enqueue a response
   */
  fun enqueue(mockResponse: MockResponse)

  /**
   * Returns a request from the recorded requests or throws if no request has been received
   */
  fun takeRequest(): MockRecordedRequest
}


@ApolloExperimental
expect class MockServer() : MockServerInterface {
}