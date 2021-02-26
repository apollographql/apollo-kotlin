package com.apollographql.apollo3.integration

import HeroNameQuery
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.NormalizedCache
import com.apollographql.apollo3.cache.normalized.MemoryCache
import com.apollographql.apollo3.ApolloQueryRequest
import com.apollographql.apollo3.interceptor.cache.FetchPolicy
import com.apollographql.apollo3.interceptor.cache.normalizedCache
import com.apollographql.apollo3.interceptor.cache.fetchPolicy
import com.apollographql.apollo3.interceptor.cache.isFromCache
import com.apollographql.apollo3.testing.MockNetworkTransport
import com.apollographql.apollo3.testing.TestLoggerExecutor
import com.apollographql.apollo3.testing.runBlocking
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

@Suppress("EXPERIMENTAL_API_USAGE")
class CacheInterceptorTest {
  private lateinit var networkTransport: MockNetworkTransport
  private lateinit var apolloClient: ApolloClient
  private lateinit var cache: NormalizedCache

  @BeforeTest
  fun setUp() {
    cache = MemoryCache(maxSizeBytes = Int.MAX_VALUE)
    networkTransport = MockNetworkTransport()
    apolloClient = ApolloClient.Builder()
        .networkTransport(networkTransport)
        .addInterceptor(TestLoggerExecutor)
        .normalizedCache(cache)
        .build()
  }

  @Test
  fun `CACHE_FIRST test`() {
    networkTransport.offer(fixtureResponse("HeroNameResponse.json"))

    runBlocking {
      var response = apolloClient
          .query(HeroNameQuery())
          .execute()
          .single()

      assertNotNull(response.data)
      assertFalse(response.isFromCache)

      response = apolloClient
          .query(HeroNameQuery())
          .execute()
          .single()

      assertNotNull(response.data)
      assertTrue(response.isFromCache)
    }
  }

  @Test
  fun `NETWORK_FIRST test`() {
    runBlocking {
      val request = ApolloQueryRequest.Builder(query = HeroNameQuery())
          .fetchPolicy(FetchPolicy.NETWORK_FIRST)
          .build()

      networkTransport.offer(fixtureResponse("HeroNameResponse.json"))
      var responses = apolloClient
          .query(request)
          .execute()
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertFalse(responses[0].isFromCache)

      // Now data is cached but it shouldn't be used since network will go through
      networkTransport.offer(fixtureResponse("HeroNameResponse.json"))
      responses = apolloClient
          .query(request)
          .execute()
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertFalse(responses[0].isFromCache)

      // Network error -> we should hit the cache
      networkTransport.offer("malformed")
      responses = apolloClient
          .query(request)
          .execute()
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertTrue(responses[0].isFromCache)

      // Network error and no cache -> we should get an error
      networkTransport.offer("malformed")
      cache.clearAll()
      try {
        responses = apolloClient
            .query(request)
            .execute()
            .toList()
        fail("NETWORK_FIRST should throw the network exception if nothing is in the cache")
      } catch (e: Exception) {

      }
    }
  }

  @Test
  fun `CACHE_ONLY test`() {
    runBlocking {
      var request = ApolloQueryRequest.Builder(query = HeroNameQuery())
          .build()

      // First cache the response
      networkTransport.offer(fixtureResponse("HeroNameResponse.json"))
      var responses = apolloClient
          .query(request)
          .execute()
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertFalse(responses[0].isFromCache)

      // Now make the request cache only
      request = request.newBuilder()
          .fetchPolicy(FetchPolicy.CACHE_ONLY)
          .build()

      networkTransport.offer(fixtureResponse("HeroNameResponse.json"))
      responses = apolloClient
          .query(request)
          .execute()
          .toList()

      // And make sure we don't read the network
      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertTrue(responses[0].isFromCache)
    }
  }

  @Test
  fun `NETWORK_ONLY test`() {
    runBlocking {
      val request = ApolloQueryRequest.Builder(query = HeroNameQuery())
          .fetchPolicy(FetchPolicy.NETWORK_ONLY)
          .build()

      // cache the response
      networkTransport.offer(fixtureResponse("HeroNameResponse.json"))
      var responses = apolloClient
          .query(request)
          .execute()
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertFalse(responses[0].isFromCache)

      // Offer a malformed response, it should fail
      networkTransport.offer("malformed")
      try {
        responses = apolloClient
            .query(request)
            .execute()
            .toList()
        fail("we expected a failure")
      } catch (e: Exception) {

      }
    }
  }

  @Test
  fun `cache_and_network test`() {
    runBlocking {
      var request = ApolloQueryRequest.Builder(query = HeroNameQuery())
          .fetchPolicy(FetchPolicy.CACHE_FIRST)
          .build()

      // cache the response
      networkTransport.offer(fixtureResponse("HeroNameResponse.json"))
      var responses = apolloClient
          .query(request)
          .execute()
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertFalse(responses[0].isFromCache)

      // Now make the request cache and network
      request = request.newBuilder()
          .fetchPolicy(FetchPolicy.CACHE_AND_NETWORK)
          .build()

      networkTransport.offer(fixtureResponse("HeroNameResponse.json"))
      responses = apolloClient
          .query(request)
          .execute()
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
