package com.apollographql.apollo3.integration.mockserver

expect class MockServer() {
  fun url(): String
  fun enqueue(mockResponse: MockResponse)
  fun takeRequest(): MockRecordedRequest
  fun stop()
}