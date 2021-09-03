package com.apollographql.apollo3.mockserver

import okio.ByteString.Companion.encodeUtf8

fun MockServer.enqueue(string: String, delayMs: Long = 0) {
  val byteString = string.encodeUtf8()
  enqueue(MockResponse(
      statusCode = 200,
      headers = mapOf("Content-Length" to byteString.size.toString()),
      body = byteString,
      delayMs = delayMs
  ))
}
