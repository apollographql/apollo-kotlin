package com.apollographql.apollo3.integration

import HeroNameQuery
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.ApolloRequest
import com.apollographql.apollo3.cache.normalized.MemoryCache
import com.apollographql.apollo3.cache.normalized.NormalizedCache
import com.apollographql.apollo3.interceptor.cache.FetchPolicy
import com.apollographql.apollo3.interceptor.cache.isFromCache
import com.apollographql.apollo3.interceptor.cache.normalizedCache
import com.apollographql.apollo3.network.http.ApolloHttpNetworkTransport
import com.apollographql.apollo3.testing.TestHttpEngine
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
  private lateinit var testHttpEngine: TestHttpEngine
  private lateinit var apolloClient: ApolloClient
  private lateinit var cache: NormalizedCache

  @BeforeTest
  fun setUp() {
    cache = MemoryCache(maxSizeBytes = Int.MAX_VALUE)
    testHttpEngine = TestHttpEngine()
    apolloClient = ApolloClient.Builder()
        .networkTransport(ApolloHttpNetworkTransport(serverUrl = "https://example", engine = testHttpEngine))
        .addInterceptor(TestLoggerExecutor)
        .normalizedCache(cache)
        .build()
  }

  @Test
  fun `CACHE_FIRST test`() {
    testHttpEngine.offer(fixtureResponse("HeroNameResponse.json"))

    runBlocking {
      var response = apolloClient
          .query(ApolloRequest(HeroNameQuery()))
          .single()

      assertNotNull(response.data)
      assertFalse(response.isFromCache)

      response = apolloClient
          .query(ApolloRequest(HeroNameQuery()))
          .single()

      assertNotNull(response.data)
      assertTrue(response.isFromCache)
    }
  }

  @Test
  fun `NETWORK_FIRST test`() {
    runBlocking {
      val request = ApolloRequest(HeroNameQuery())
          .withExecutionContext(FetchPolicy.NETWORK_FIRST)

      testHttpEngine.offer(fixtureResponse("HeroNameResponse.json"))
      var responses = apolloClient
          .query(request)
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertFalse(responses[0].isFromCache)

      // Now data is cached but it shouldn't be used since network will go through
      testHttpEngine.offer(fixtureResponse("HeroNameResponse.json"))
      responses = apolloClient
          .query(request)
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertFalse(responses[0].isFromCache)

      // Network error -> we should hit the cache
      testHttpEngine.offer("malformed")
      responses = apolloClient
          .query(request)
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertTrue(responses[0].isFromCache)

      // Network error and no cache -> we should get an error
      testHttpEngine.offer("malformed")
      cache.clearAll()
      try {
        responses = apolloClient
            .query(request)
              .toList()
        fail("NETWORK_FIRST should throw the network exception if nothing is in the cache")
      } catch (e: Exception) {

      }
    }
  }

  @Test
  fun `CACHE_ONLY test`() {
    runBlocking {
      var request = ApolloRequest(HeroNameQuery())

      // First cache the response
      testHttpEngine.offer(fixtureResponse("HeroNameResponse.json"))
      var responses = apolloClient
          .query(request)
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertFalse(responses[0].isFromCache)

      // Now make the request cache only
      request = request.withExecutionContext(FetchPolicy.CACHE_ONLY)

      testHttpEngine.offer(fixtureResponse("HeroNameResponse.json"))
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
    runBlocking {
      val request = ApolloRequest(HeroNameQuery())
          .withExecutionContext(FetchPolicy.NETWORK_ONLY)

      // cache the response
      testHttpEngine.offer(fixtureResponse("HeroNameResponse.json"))
      var responses = apolloClient
          .query(request)
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertFalse(responses[0].isFromCache)

      // Offer a malformed response, it should fail
      testHttpEngine.offer("malformed")
      try {
        responses = apolloClient
            .query(request)
              .toList()
        fail("we expected a failure")
      } catch (e: Exception) {

      }
    }
  }

  @Test
  fun `cache_and_network test`() {
    runBlocking {
      var request = ApolloRequest(HeroNameQuery())
          .withExecutionContext(FetchPolicy.CACHE_FIRST)

      // cache the response
      testHttpEngine.offer(fixtureResponse("HeroNameResponse.json"))
      var responses = apolloClient
          .query(request)
          .toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertFalse(responses[0].isFromCache)

      // Now make the request cache and network
      request = request.withExecutionContext(FetchPolicy.CACHE_AND_NETWORK)

      testHttpEngine.offer(fixtureResponse("HeroNameResponse.json"))
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
