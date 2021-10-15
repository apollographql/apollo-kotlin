package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.AnyAdapter
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.toJson
import com.apollographql.apollo3.cache.ApolloCacheHeaders
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.cacheHeaders
import com.apollographql.apollo3.cache.normalized.doNotStore
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.store
import com.apollographql.apollo3.cache.normalized.storePartialResponses
import com.apollographql.apollo3.exception.CacheMissException
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.enqueue
import com.apollographql.apollo3.testing.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class CacheFlagsTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient
  private lateinit var store: ApolloStore

  private suspend fun setUp() {
    store = ApolloStore(MemoryCacheFactory())
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).store(store).build()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun doNotStore() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))
    mockServer.enqueue(query, data)

    apolloClient.query(ApolloRequest.Builder(query).doNotStore(true).build())

    // Since the previous request was not stored, this should fail
    assertFailsWith(CacheMissException::class) {
      apolloClient.query(ApolloRequest.Builder(query).fetchPolicy(FetchPolicy.CacheOnly).build())
    }
  }

  @Test
  fun testEvictAfterRead() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))
    mockServer.enqueue(query, data)

    // Store the data
    apolloClient.query(ApolloRequest.Builder(query).fetchPolicy(FetchPolicy.NetworkOnly).build())

    // This should work and evict the entries
    val response = apolloClient.query(
        ApolloRequest.Builder(query)
            .fetchPolicy(FetchPolicy.CacheOnly)
            .cacheHeaders(
                CacheHeaders.builder().addHeader(ApolloCacheHeaders.EVICT_AFTER_READ, "true").build()
            )
              .build()
    )
    assertEquals("R2-D2", response.data?.hero?.name)

    // Second time should fail
    assertFailsWith(CacheMissException::class) {
      apolloClient.query(ApolloRequest.Builder(query).fetchPolicy(FetchPolicy.CacheOnly).build())
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
  fun partialResponsesAreNotStored() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = HeroNameQuery()
    mockServer.enqueue(AnyAdapter.toJson(partialResponse))

    // this should not store the response
    apolloClient.query(ApolloRequest.Builder(query).build())

    assertFailsWith(CacheMissException::class) {
      apolloClient.query(ApolloRequest.Builder(query).fetchPolicy(FetchPolicy.CacheOnly).build())
    }
  }

  @Test
  fun storePartialResponse() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = HeroNameQuery()
    mockServer.enqueue(AnyAdapter.toJson(partialResponse))

    // this should not store the response
    apolloClient.query(ApolloRequest.Builder(query).storePartialResponses(true).build())

    val response = apolloClient.query(ApolloRequest.Builder(query).fetchPolicy(FetchPolicy.CacheOnly).build())
    assertNotNull(response.data)
  }
}