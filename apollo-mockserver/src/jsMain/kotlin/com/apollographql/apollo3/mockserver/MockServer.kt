package com.apollographql.apollo3.mockserver

actual class MockServer {
  actual fun url(): String {
    TODO("MockServer.url()")
  }

  actual fun enqueue(mockResponse: MockResponse) {
    TODO("MockServer.enqueue()")
  }

  actual fun takeRequest(): MockRecordedRequest {
    TODO("MockServer.takeRequest()")
  }

  actual fun stop() {
    TODO("MockServer.stop()")
  }
}