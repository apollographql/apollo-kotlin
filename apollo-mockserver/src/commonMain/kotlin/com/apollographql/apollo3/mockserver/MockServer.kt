package com.apollographql.apollo3.mockserver

import okio.ByteString.Companion.encodeUtf8

expect class MockServer() {
  fun url(): String
  fun enqueue(mockResponse: MockResponse)

  /**
   * Returns a request from the recorded requests or throws if no request has been received
   */
  fun takeRequest(): MockRecordedRequest
  fun stop()
}

