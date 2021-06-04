package com.apollographql.apollo3.testing

import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.composeResponseBody
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import okio.ByteString.Companion.encodeUtf8

fun <D : Operation.Data> MockServer.enqueue(
    operation: Operation<D>,
    data: D,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    delayMs: Long = 0
) {
  val json = operation.composeResponseBody(data, customScalarAdapters = customScalarAdapters)
  enqueue(json, delayMs)
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
