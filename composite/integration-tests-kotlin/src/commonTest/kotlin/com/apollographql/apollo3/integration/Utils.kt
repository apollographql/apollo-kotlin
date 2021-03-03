package com.apollographql.apollo3.integration

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.toResponse
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.CacheKeyResolver
import com.apollographql.apollo3.network.http.ApolloHttpNetworkTransport
import com.apollographql.apollo3.network.http.HttpResponse
import com.apollographql.apollo3.testing.TestHttpEngine
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8


internal fun TestApolloClient(httpEngine: TestHttpEngine): ApolloClient {
  return ApolloClient.Builder()
      .networkTransport(
          ApolloHttpNetworkTransport(
              serverUrl = "https://example.com",
              engine = httpEngine
          )
      ).build()
}

class IdFieldCacheKeyResolver : CacheKeyResolver() {
  override fun fromFieldRecordSet(field: ResponseField, recordSet: Map<String, Any?>): CacheKey {
    val id = recordSet["id"]
    return if (id != null) {
      formatCacheKey(id.toString())
    } else {
      formatCacheKey(null)
    }
  }

  override fun fromFieldArguments(field: ResponseField, variables: Operation.Variables): CacheKey {
    val id = field.resolveArgument("id", variables)
    return if (id != null) {
      formatCacheKey(id.toString())
    } else {
      formatCacheKey(null)
    }
  }

  private fun formatCacheKey(id: String?): CacheKey {
    return if (id == null || id.isEmpty()) {
      CacheKey.NO_KEY
    } else {
      CacheKey(id)
    }
  }
}

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

