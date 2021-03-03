package com.apollographql.apollo3.integration

expect class MockServer() {
  fun url(): String
  fun enqueue(mockResponse: MockResponse)
  fun takeRequest(): RecordedRequest
  fun stop()
}