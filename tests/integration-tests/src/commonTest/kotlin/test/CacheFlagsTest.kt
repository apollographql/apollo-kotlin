package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.AnyAdapter
import com.apollographql.apollo3.api.toJsonString
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.api.ApolloCacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
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

@OptIn(ApolloExperimental::class)
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

    apolloClient.query(query).doNotStore(true).execute()

    // Since the previous request was not stored, this should fail
    assertFailsWith(CacheMissException::class) {
      apolloClient.query(query).fetchPolicy(FetchPolicy.CacheOnly).execute()
    }
  }

  @Test
  fun testEvictAfterRead() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))
    mockServer.enqueue(query, data)

    // Store the data
    apolloClient.query(query).fetchPolicy(FetchPolicy.NetworkOnly).execute()

    // This should work and evict the entries
    val response = apolloClient.query(query)
            .fetchPolicy(FetchPolicy.CacheOnly)
            .cacheHeaders(CacheHeaders.builder().addHeader(ApolloCacheHeaders.EVICT_AFTER_READ, "true").build())
        .execute()

    assertEquals("R2-D2", response.data?.hero?.name)

    // Second time should fail
    assertFailsWith(CacheMissException::class) {
      apolloClient.query(query).fetchPolicy(FetchPolicy.CacheOnly).execute()
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
    mockServer.enqueue(AnyAdapter.toJsonString(partialResponse))

    // this should not store the response
    apolloClient.query(query).execute()

    assertFailsWith(CacheMissException::class) {
      apolloClient.query(query).fetchPolicy(FetchPolicy.CacheOnly).execute()
    }
  }

  @Test
  fun storePartialResponse() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = HeroNameQuery()
    mockServer.enqueue(AnyAdapter.toJsonString(partialResponse))

    // this should not store the response
    apolloClient.query(query).storePartialResponses(true).execute()

    val response = apolloClient.query(query).fetchPolicy(FetchPolicy.CacheOnly).execute()
    assertNotNull(response.data)
  }
}