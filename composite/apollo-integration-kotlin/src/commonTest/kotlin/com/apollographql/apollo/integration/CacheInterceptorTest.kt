package com.apollographql.apollo.integration

import HeroNameQuery
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.simple.MapNormalizedCache
import com.apollographql.apollo.interceptor.ApolloQueryRequest
import com.apollographql.apollo.interceptor.cache.ApolloCacheInterceptor
import com.apollographql.apollo.interceptor.cache.ApolloStore
import com.apollographql.apollo.interceptor.cache.NormalizedCachePolicy
import com.apollographql.apollo.interceptor.cache.fromCache
import com.apollographql.apollo.interceptor.cache.normalizedCache
import com.apollographql.apollo.interceptor.cache.normalizedCachePolicy
import com.apollographql.apollo.testing.MockNetworkTransport
import com.apollographql.apollo.testing.TestLoggerExecutor
import com.apollographql.apollo.testing.runBlocking
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

  @BeforeTest
  fun setUp() {
    networkTransport = MockNetworkTransport()
    apolloClient = ApolloClient.Builder()
        .networkTransport(networkTransport)
        .addInterceptor(TestLoggerExecutor)
        .normalizedCache(MapNormalizedCache())
        .build()
  }

  @Test
  fun `cache_first test`() {
    networkTransport.offer(fixtureResponse("HeroNameResponse.json"))

    runBlocking {
      var response = apolloClient
          .query(HeroNameQuery())
          .execute()
          .single()

      assertNotNull(response.data)
      assertFalse(response.fromCache)

      response = apolloClient
          .query(HeroNameQuery())
          .execute()
          .single()

      assertNotNull(response.data)
      assertTrue(response.fromCache)
    }
  }

  @Test
  fun `network_first test`() {
    runBlocking {
      val request = ApolloQueryRequest.Builder(query = HeroNameQuery())
          .normalizedCachePolicy(NormalizedCachePolicy.NETWORK_FIRST)
          .build()

      networkTransport.offer(fixtureResponse("HeroNameResponse.json"))
      var responses = apolloClient
          .query(request)
          .execute()
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertFalse(responses[0].fromCache)

      // Now data is cached but it shouldn't be used since network will go through
      networkTransport.offer(fixtureResponse("HeroNameResponse.json"))
      responses = apolloClient
          .query(request)
          .execute()
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertFalse(responses[0].fromCache)

      // Do not offer a response and we should hit the cache
      networkTransport.offer("malformed")
      responses = apolloClient
          .query(request)
          .execute()
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertTrue(responses[0].fromCache)
    }
  }

  @Test
  fun `cache_only test`() {
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
      assertFalse(responses[0].fromCache)

      // Now make the request cache only
      request = request.newBuilder()
          .normalizedCachePolicy(NormalizedCachePolicy.CACHE_ONLY)
          .build()

      networkTransport.offer(fixtureResponse("HeroNameResponse.json"))
      responses = apolloClient
          .query(request)
          .execute()
          .toList()

      // And make sure we don't read the network
      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertTrue(responses[0].fromCache)
    }
  }

  @Test
  fun `network_only test`() {
    runBlocking {
      val request = ApolloQueryRequest.Builder(query = HeroNameQuery())
          .normalizedCachePolicy(NormalizedCachePolicy.NETWORK_ONLY)
          .build()

      // cache the response
      networkTransport.offer(fixtureResponse("HeroNameResponse.json"))
      var responses = apolloClient
          .query(request)
          .execute()
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertFalse(responses[0].fromCache)

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
          .normalizedCachePolicy(NormalizedCachePolicy.CACHE_FIRST)
          .build()

      // cache the response
      networkTransport.offer(fixtureResponse("HeroNameResponse.json"))
      var responses = apolloClient
          .query(request)
          .execute()
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertFalse(responses[0].fromCache)

      // Now make the request cache and network
      request = request.newBuilder()
          .normalizedCachePolicy(NormalizedCachePolicy.CACHE_AND_NETWORK)
          .build()

      networkTransport.offer(fixtureResponse("HeroNameResponse.json"))
      responses = apolloClient
          .query(request)
          .execute()
          .toList()

      // We should have 2 responses
      assertEquals(2, responses.size)
      assertNotNull(responses[0].data)
      assertTrue(responses[0].fromCache)
      assertNotNull(responses[1].data)
      assertFalse(responses[1].fromCache)
    }
  }
}
