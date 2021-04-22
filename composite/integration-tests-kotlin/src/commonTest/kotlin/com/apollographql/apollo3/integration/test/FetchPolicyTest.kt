package com.apollographql.apollo3.integration.test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.ApolloRequest
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.CacheKeyResolver
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.internal.ApolloStore
import com.apollographql.apollo3.integration.mockserver.MockServer
import com.apollographql.apollo3.integration.enqueue
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.interceptor.cache.FetchPolicy
import com.apollographql.apollo3.interceptor.cache.isFromCache
import com.apollographql.apollo3.interceptor.cache.normalizedCache
import com.apollographql.apollo3.testing.runWithMainLoop
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class FetchPolicyTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient
  private lateinit var store: ApolloStore

  @BeforeTest
  fun setUp() {
    store = ApolloStore(MemoryCacheFactory(maxSizeBytes = Int.MAX_VALUE), CacheKeyResolver.DEFAULT)
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .normalizedCache(store)
        .build()
  }

  @Test
  fun `CACHE_FIRST test`() {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Data.Hero("R2-D2"))
    mockServer.enqueue(query, data)

    runWithMainLoop {
      var response = apolloClient
          .query(query)
          .single()

      assertNotNull(response.data)
      assertFalse(response.isFromCache)

      response = apolloClient
          .query(query)
          .single()

      assertNotNull(response.data)
      assertTrue(response.isFromCache)
    }
  }

  @Test
  fun `NETWORK_FIRST test`() {
    runWithMainLoop {
      val query = HeroNameQuery()
      val data = HeroNameQuery.Data(HeroNameQuery.Data.Hero("R2-D2"))

      val request = ApolloRequest(query).withExecutionContext(FetchPolicy.NetworkFirst)

      mockServer.enqueue(query, data)
      var responses = apolloClient
          .query(request)
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertFalse(responses[0].isFromCache)

      // Now data is cached but it shouldn't be used since network will go through
      mockServer.enqueue(query, data)
      responses = apolloClient
          .query(request)
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertFalse(responses[0].isFromCache)

      // Network error -> we should hit the cache
      mockServer.enqueue("malformed")
      responses = apolloClient
          .query(request)
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertTrue(responses[0].isFromCache)

      // Network error and no cache -> we should get an error
      mockServer.enqueue("malformed")
      store.clearAll()
      try {
        apolloClient
            .query(request)
            .toList()
        fail("NETWORK_FIRST should throw the network exception if nothing is in the cache")
      } catch (e: Exception) {

      }
    }
  }

  @Test
  fun `CACHE_ONLY test`() {
    runWithMainLoop {
      val query = HeroNameQuery()
      val data = HeroNameQuery.Data(HeroNameQuery.Data.Hero("R2-D2"))

      var request = ApolloRequest(query)

      // First cache the response
      mockServer.enqueue(query, data)
      var responses = apolloClient
          .query(request)
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertFalse(responses[0].isFromCache)

      // Now make the request cache only
      request = request.withExecutionContext(FetchPolicy.CacheOnly)

      responses = apolloClient
          .query(request)
          .toList()

      // And make sure we don't read the network
      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertTrue(responses[0].isFromCache)
    }
  }

  @Test
  fun `NETWORK_ONLY test`() {
    runWithMainLoop {
      val query = HeroNameQuery()
      val data = HeroNameQuery.Data(HeroNameQuery.Data.Hero("R2-D2"))

      val request = ApolloRequest(query)
          .withExecutionContext(FetchPolicy.NetworkOnly)

      // cache the response
      mockServer.enqueue(query, data)
      val responses = apolloClient
          .query(request)
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertFalse(responses[0].isFromCache)

      // Offer a malformed response, it should fail
      mockServer.enqueue("malformed")
      val result = kotlin.runCatching {
        apolloClient
            .query(request)
            .toList()
        fail("we expected a failure")
      }

      assertTrue(result.isFailure)
    }
  }

  @Test
  fun `cache_and_network test`() {
    runWithMainLoop {
      val query = HeroNameQuery()
      val data = HeroNameQuery.Data(HeroNameQuery.Data.Hero("R2-D2"))

      var request = ApolloRequest(query)
          .withExecutionContext(FetchPolicy.CacheFirst)

      // cache the response
      mockServer.enqueue(query, data)
      var responses = apolloClient
          .query(request)
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertFalse(responses[0].isFromCache)

      // Now make the request cache and network
      request = request.withExecutionContext(FetchPolicy.CacheAndNetwork)

      mockServer.enqueue(query, data)
      responses = apolloClient
          .query(request)
          .toList()

      // We should have 2 responses
      assertEquals(2, responses.size)
      assertNotNull(responses[0].data)
      assertTrue(responses[0].isFromCache)
      assertNotNull(responses[1].data)
      assertFalse(responses[1].isFromCache)
    }
  }
}
