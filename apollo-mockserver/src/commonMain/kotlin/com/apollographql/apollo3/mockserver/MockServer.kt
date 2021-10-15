package com.apollographql.apollo3.mockserver

import okio.ByteString.Companion.encodeUtf8

expect class MockServer() {
  /**
   * Returns the root url for this server
   *
   * It will block until a port is found to listen to
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

