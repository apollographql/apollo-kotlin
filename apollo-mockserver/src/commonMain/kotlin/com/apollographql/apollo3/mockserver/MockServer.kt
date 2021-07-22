package com.apollographql.apollo3.mockserver

import okio.ByteString.Companion.encodeUtf8

expect class MockServer() {
  suspend fun url(): String
  fun enqueue(mockResponse: MockResponse)

  /**
   * Returns a request from the recorded requests or throws if no request has been received
   */
  fun takeRequest(): MockRecordedRequest
  suspend fun stop()
}

fun MockServer.enqueue(string: String, delayMs: Long = 0) {
  val byteString = string.encodeUtf8()
  enqueue(MockResponse(
      statusCode = 200,
      headers = mapOf("Content-Length" to byteString.size.toString()),
      body = byteString,
      delayMs = delayMs
  ))
}
