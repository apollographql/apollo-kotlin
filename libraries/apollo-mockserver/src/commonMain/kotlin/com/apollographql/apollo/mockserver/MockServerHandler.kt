package com.apollographql.apollo.mockserver

interface MockServerHandler {
  /**
   * Handles the given [MockRequestBase].
   *
   * This method is called from one or several background threads and must be thread-safe.
   */
  fun handle(request: MockRequestBase): MockResponse
}