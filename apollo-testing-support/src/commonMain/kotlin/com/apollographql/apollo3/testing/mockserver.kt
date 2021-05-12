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
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty
) {
  val json = operation.composeResponseBody(data, customScalarAdapters = customScalarAdapters)
  enqueue(json)
}

fun MockServer.enqueue(string: String) {
  val byteString = string.encodeUtf8()
  enqueue(MockResponse(
      statusCode = 200,
      headers = mapOf("Content-Length" to byteString.size.toString()),
      body = byteString
  ))
}
