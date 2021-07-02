package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.AnyAdapter
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.exception.CacheMissException
import com.apollographql.apollo3.api.toJson
import com.apollographql.apollo3.cache.ApolloCacheHeaders
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.cache.normalized.CACHE_FLAG_DO_NOT_STORE
import com.apollographql.apollo3.cache.normalized.CACHE_FLAG_STORE_PARTIAL_RESPONSE
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.withCacheFlags
import com.apollographql.apollo3.cache.normalized.withCacheHeaders
import com.apollographql.apollo3.cache.normalized.withFetchPolicy
import com.apollographql.apollo3.cache.normalized.withStore
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.enqueue
import com.apollographql.apollo3.testing.runWithMainLoop
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class CacheFlagsTest {
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
  fun doNotStore() {
    runWithMainLoop {
      val query = HeroNameQuery()
      val data = HeroNameQuery.Data(HeroNameQuery.Data.Hero("R2-D2"))
      mockServer.enqueue(query, data)

      apolloClient.query(ApolloRequest(query).withCacheFlags(CACHE_FLAG_DO_NOT_STORE))

      // Since the previous request was not stored, this should fail
      assertFailsWith(CacheMissException::class) {
        apolloClient.query(ApolloRequest(query).withFetchPolicy(FetchPolicy.CacheOnly))
      }
    }
  }

  @Test
  fun testEvictAfterRead() {
    runWithMainLoop {
      val query = HeroNameQuery()
      val data = HeroNameQuery.Data(HeroNameQuery.Data.Hero("R2-D2"))
      mockServer.enqueue(query, data)

      // Store the data
      apolloClient.query(ApolloRequest(query).withFetchPolicy(FetchPolicy.NetworkOnly))

      // This should work and evict the entries
      val response = apolloClient.query(
          ApolloRequest(query)
              .withFetchPolicy(FetchPolicy.CacheOnly)
              .withCacheHeaders(
                  CacheHeaders.builder().addHeader(ApolloCacheHeaders.EVICT_AFTER_READ, "true").build()
              )
      )
      assertEquals("R2-D2", response.data?.hero?.name)

      // Second time should fail
      assertFailsWith(CacheMissException::class) {
        apolloClient.query(ApolloRequest(query).withFetchPolicy(FetchPolicy.CacheOnly))
      }
    }
  }

  private val partialResponse = mapOf(
      "data" to mapOf(
          "hero" to null
      ),
      "errors" to listOf(
          mapOf(
              "message" to "An error Happened",
              "locations" to listOf(
                  mapOf(
                      "line" to 0,
                      "column" to 0
                  )
              )
          )
      )
  )

  @Test
  fun partialResponsesAreNotStored() {
    runWithMainLoop {
      val query = HeroNameQuery()
      mockServer.enqueue(AnyAdapter.toJson(partialResponse))

      // this should not store the response
      apolloClient.query(ApolloRequest(query))

      assertFailsWith(CacheMissException::class) {
        apolloClient.query(ApolloRequest(query).withFetchPolicy(FetchPolicy.CacheOnly))
      }
    }
  }

  @Test
  fun storePartialResponse() {
    runWithMainLoop {
      val query = HeroNameQuery()
      mockServer.enqueue(AnyAdapter.toJson(partialResponse))

      // this should not store the response
      apolloClient.query(ApolloRequest(query).withCacheFlags(CACHE_FLAG_STORE_PARTIAL_RESPONSE))

      val response = apolloClient.query(ApolloRequest(query).withFetchPolicy(FetchPolicy.CacheOnly))
      assertNotNull(response.data)
    }
  }
}