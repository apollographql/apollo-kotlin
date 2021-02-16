package com.apollographql.apollo3

import com.apollographql.apollo3.integration.normalizer.GetJsonScalarQuery
import com.apollographql.apollo3.Utils.cacheAndAssertCachedResponse
import com.apollographql.apollo3.Utils.immediateExecutor
import com.apollographql.apollo3.Utils.immediateExecutorService
import com.apollographql.apollo3.api.BigDecimal
import com.apollographql.apollo3.api.BuiltinCustomScalarAdapters
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.integration.normalizer.type.CustomScalars
import com.google.common.truth.Truth.assertThat
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test

class JsonScalarTest {
  @Test
  @Throws(Exception::class)
  fun jsonScalar() {
    val server = MockWebServer()

    val apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .addCustomScalarAdapter(CustomScalars.Json, BuiltinCustomScalarAdapters.MAP_ADAPTER)
        .normalizedCache(MemoryCacheFactory(maxSizeBytes = Int.MAX_VALUE), IdFieldCacheKeyResolver())
        .build()

    cacheAndAssertCachedResponse(
        server,
        "JsonScalar.json",
        apolloClient.query(GetJsonScalarQuery())
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      val expectedMap = mapOf(
          "obj" to mapOf("key" to "value"),
          "list" to listOf(0, 1, 2)
      )
      assertThat(response.data!!.json).isEqualTo(expectedMap)
    }

    // Trigger a merge
    cacheAndAssertCachedResponse(
        server,
        "JsonScalarModified.json",
        apolloClient.query(GetJsonScalarQuery())
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      val expectedMap = mapOf(
          "obj" to mapOf("key2" to "value2"),
      )
      assertThat(response.data!!.json).isEqualTo(expectedMap)
      true
    }
  }
}
