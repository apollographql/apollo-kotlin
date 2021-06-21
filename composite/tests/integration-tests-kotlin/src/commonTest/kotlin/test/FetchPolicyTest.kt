package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.exception.ApolloCompositeException
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.CacheResolver
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.interceptor.cache.FetchPolicy
import com.apollographql.apollo3.interceptor.cache.isFromCache
import com.apollographql.apollo3.interceptor.cache.queryCacheAndNetwork
import com.apollographql.apollo3.interceptor.cache.withFetchPolicy
import com.apollographql.apollo3.interceptor.cache.withStore
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.enqueue
import com.apollographql.apollo3.testing.runWithMainLoop
import kotlinx.coroutines.flow.toList
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
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
    store = ApolloStore(MemoryCacheFactory())
    mockServer = MockServer()
    apolloClient = ApolloClient(mockServer.url()).withStore(store)
  }

  @Test
  fun cacheFirst() {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Data.Hero("R2-D2"))
    mockServer.enqueue(query, data)

    // Cache first is also the default, no need to set the fetchPolicy
    runWithMainLoop {
      // First query should hit the network and save in cache
      val request = ApolloRequest(query).withFetchPolicy(FetchPolicy.NetworkFirst)
      var response = apolloClient.query(request)

      assertNotNull(response.data)
      assertFalse(response.isFromCache)

      // Second query should only hit the cache
      response = apolloClient.query(query)

      assertNotNull(response.data)
      assertTrue(response.isFromCache)

      // Clear the store and offer a malformed response, we should get a composite error
      store.clearAll()
      mockServer.enqueue("malformed")
      try {
        apolloClient.query(query)
        fail("we expected the query to fail")
      } catch (e: Exception) {
        assertTrue(e is ApolloCompositeException)
      }
    }
  }

  @Test
  fun networkFirst() {
    runWithMainLoop {
      val query = HeroNameQuery()
      val data = HeroNameQuery.Data(HeroNameQuery.Data.Hero("R2-D2"))

      val request = ApolloRequest(query).withFetchPolicy(FetchPolicy.NetworkFirst)

      // First query should hit the network and save in cache
      mockServer.enqueue(query, data)
      var response = apolloClient.query(request)

      assertNotNull(response.data)
      assertFalse(response.isFromCache)

      // Now data is cached but it shouldn't be used since network will go through
      mockServer.enqueue(query, data)
      response = apolloClient.query(request)

      assertNotNull(response.data)
      assertFalse(response.isFromCache)

      // Network error -> we should hit now the cache
      mockServer.enqueue("malformed")
      response = apolloClient.query(request)

      assertNotNull(response.data)
      assertTrue(response.isFromCache)

      // Network error and no cache -> we should get an error
      mockServer.enqueue("malformed")
      store.clearAll()
      try {
        apolloClient.query(request)
        fail("NETWORK_FIRST should throw the network exception if nothing is in the cache")
      } catch (e: Exception) {

      }
    }
  }

  @Test
  fun cacheOnly() {
    runWithMainLoop {
      val query = HeroNameQuery()
      val data = HeroNameQuery.Data(HeroNameQuery.Data.Hero("R2-D2"))

      var request = ApolloRequest(query)

      // First query should hit the network and save in cache
      mockServer.enqueue(query, data)
      var response = apolloClient.query(request)

      assertNotNull(response.data)
      assertFalse(response.isFromCache)

      request = request.withFetchPolicy(FetchPolicy.CacheOnly)

      // Second query should only hit the cache
      response = apolloClient.query(request)

      // And make sure we don't read the network
      assertNotNull(response.data)
      assertTrue(response.isFromCache)
    }
  }

  @Test
  fun networkOnly() {
    runWithMainLoop {
      val query = HeroNameQuery()
      val data = HeroNameQuery.Data(HeroNameQuery.Data.Hero("R2-D2"))

      val request = ApolloRequest(query).withFetchPolicy(FetchPolicy.NetworkOnly)

      // First query should hit the network and save in cache
      mockServer.enqueue(query, data)
      val response = apolloClient.query(request)

      assertNotNull(response.data)
      assertFalse(response.isFromCache)

      // Offer a malformed response, it should fail
      mockServer.enqueue("malformed")
      try {
        apolloClient.query(request)
        fail("we expected a failure")
      } catch (e: Exception) {

      }
    }
  }

  @Test
  fun queryCacheAndNetwork() {
    runWithMainLoop {
      val query = HeroNameQuery()
      val data = HeroNameQuery.Data(HeroNameQuery.Data.Hero("R2-D2"))

      // Initial state: everything fails
      // Cache Error + Network Error => Error
      mockServer.enqueue(MockResponse(statusCode = 500))
      assertFailsWith(ApolloCompositeException::class) {
        apolloClient.queryCacheAndNetwork(query).toList()
      }

      // Make the network return something
      // Cache Error + Nework Success => 1 response
      mockServer.enqueue(query, data)
      var responses =  apolloClient.queryCacheAndNetwork(query).toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertFalse(responses[0].isFromCache)
      assertEquals("R2-D2", responses[0].data?.hero?.name)

      // Now cache is populated but make the network fail again
      // Cache Success + Network Error => 1 response
      mockServer.enqueue(MockResponse(statusCode = 500))
      responses = apolloClient.queryCacheAndNetwork(query).toList()

      assertEquals(1, responses.size)
      assertNotNull(responses[0].data)
      assertTrue(responses[0].isFromCache)
      assertEquals("R2-D2", responses[0].data?.hero?.name)

      // Cache Success + Network Success => 1 response
      mockServer.enqueue(query, data)
      responses = apolloClient.queryCacheAndNetwork(query).toList()

      assertEquals(2, responses.size)
      assertNotNull(responses[0].data)
      assertTrue(responses[0].isFromCache)
      assertNotNull(responses[1].data)
      assertFalse(responses[1].isFromCache)
    }
  }
}
