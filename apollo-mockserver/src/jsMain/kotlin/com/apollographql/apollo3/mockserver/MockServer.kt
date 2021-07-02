package com.apollographql.apollo3.mockserver

actual class MockServer {
  actual fun url(): String {
    TODO()
  }

  actual fun enqueue(mockResponse: MockResponse) {
    TODO()
  }

  actual fun takeRequest(): MockRecordedRequest {
    TODO()
  }

  actual fun stop() {
    TODO()
  }
}