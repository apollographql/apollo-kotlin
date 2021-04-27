package com.apollographql.apollo3.integration.test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.ApolloRequest
import com.apollographql.apollo3.api.AnyResponseAdapter
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.integration.IdFieldCacheKeyResolver
import com.apollographql.apollo3.integration.assertEquals2
import com.apollographql.apollo3.integration.enqueue
import com.apollographql.apollo3.integration.mockserver.MockServer
import com.apollographql.apollo3.integration.normalizer.GetJsonScalarQuery
import com.apollographql.apollo3.integration.normalizer.type.Types
import com.apollographql.apollo3.integration.readResource
import com.apollographql.apollo3.interceptor.cache.FetchPolicy
import com.apollographql.apollo3.interceptor.cache.normalizedCache
import com.apollographql.apollo3.interceptor.cache.withFetchPolicy
import com.apollographql.apollo3.testing.runWithMainLoop
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class JsonScalarTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient
  private lateinit var store: ApolloStore

  @BeforeTest
  fun setUp() {
    store = ApolloStore(MemoryCacheFactory(maxSizeBytes = Int.MAX_VALUE), IdFieldCacheKeyResolver)
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addScalarTypeAdapter(Types.Json, AnyResponseAdapter)
        .normalizedCache(store)
        .build()
  }

  // see https://github.com/apollographql/apollo-android/issues/2854
  @Test
  fun jsonScalar() = runWithMainLoop {
    mockServer.enqueue(readResource("JsonScalar.json"))
    var response = apolloClient.query(GetJsonScalarQuery())

    assertFalse(response.hasErrors())
    var expectedMap = mapOf(
        "obj" to mapOf("key" to "value"),
        "list" to listOf(0, 1, 2)
    )
    assertEquals(expectedMap, response.data!!.json)

    // Trigger a merge
    mockServer.enqueue(readResource("JsonScalarModified.json"))
    apolloClient.query(ApolloRequest(GetJsonScalarQuery()).withFetchPolicy(FetchPolicy.NetworkFirst))
    response = apolloClient.query(ApolloRequest(GetJsonScalarQuery()).withFetchPolicy(FetchPolicy.CacheOnly))

    assertFalse(response.hasErrors())

    expectedMap = mapOf(
        "obj" to mapOf("key2" to "value2"),
    )
    assertEquals(expectedMap, response.data!!.json)
  }
}
