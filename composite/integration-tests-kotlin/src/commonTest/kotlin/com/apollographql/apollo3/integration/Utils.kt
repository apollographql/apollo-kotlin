package com.apollographql.apollo3.integration

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.CacheKeyResolver
import com.apollographql.apollo3.network.http.ApolloHttpNetworkTransport
import com.apollographql.apollo3.testing.TestHttpEngine


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
