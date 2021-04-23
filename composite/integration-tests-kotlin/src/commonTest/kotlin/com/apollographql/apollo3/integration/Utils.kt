package com.apollographql.apollo3.integration

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.toResponse
import com.apollographql.apollo3.integration.mockserver.MockResponse
import com.apollographql.apollo3.integration.mockserver.MockServer
import okio.ByteString.Companion.encodeUtf8


fun <D : Operation.Data> MockServer.enqueue(
    operation: Operation<D>,
    data: D,
    responseAdapterCache: ResponseAdapterCache = ResponseAdapterCache.DEFAULT
) {
  val json = operation.toResponse(data, responseAdapterCache = responseAdapterCache)
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

fun readResource(name: String) = readTestFixture("resources/$name")

