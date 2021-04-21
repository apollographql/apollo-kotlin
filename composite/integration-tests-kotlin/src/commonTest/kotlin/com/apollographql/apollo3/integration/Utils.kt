package com.apollographql.apollo3.integration

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.toResponse
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.CacheKeyResolver
import com.apollographql.apollo3.integration.mockserver.MockResponse
import com.apollographql.apollo3.integration.mockserver.MockServer
import com.apollographql.apollo3.network.http.ApolloHttpNetworkTransport
import com.apollographql.apollo3.testing.TestHttpEngine
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

