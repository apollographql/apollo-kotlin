package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.Error
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
import com.apollographql.apollo3.testing.TestNetworkTransport
import com.apollographql.apollo3.testing.runTest
import com.apollographql.apollo3.testing.testNetworkTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@OptIn(ApolloExperimental::class)
class CacheFlagsTest {
  private lateinit var apolloClient: ApolloClient
  private lateinit var store: ApolloStore

  private fun setUp() {
    store = ApolloStore(MemoryCacheFactory())
    apolloClient = ApolloClient.Builder().networkTransport(TestNetworkTransport()).store(store).build()
  }

  @Test
  fun doNotStore() = runTest(before = { setUp() }) {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))
    apolloClient.testNetworkTransport.register(query, data)

    apolloClient.query(query).doNotStore(true).execute()

    // Since the previous request was not stored, this should fail
    assertFailsWith(CacheMissException::class) {
      apolloClient.query(query).fetchPolicy(FetchPolicy.CacheOnly).execute()
    }
  }

  @Test
  fun testEvictAfterRead() = runTest(before = { setUp() }) {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))
    apolloClient.testNetworkTransport.register(query, data)

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

  private val partialResponseData = HeroNameQuery.Data(null)
  private val partialResponseErrors = listOf(Error(
      message = "An error Happened",
      locations = listOf(Error.Location(0, 0)),
      path = null, extensions = null, nonStandardFields = null))


  @Test
  fun partialResponsesAreNotStored() = runTest(before = { setUp() }) {
    val query = HeroNameQuery()
    apolloClient.testNetworkTransport.register(query, partialResponseData, partialResponseErrors)

    // this should not store the response
    apolloClient.query(query).execute()

    assertFailsWith(CacheMissException::class) {
      apolloClient.query(query).fetchPolicy(FetchPolicy.CacheOnly).execute()
    }
  }

  @Test
  fun storePartialResponse() = runTest(before = { setUp() }) {
    val query = HeroNameQuery()
    apolloClient.testNetworkTransport.register(query, partialResponseData, partialResponseErrors)

    // this should not store the response
    apolloClient.query(query).storePartialResponses(true).execute()

    val response = apolloClient.query(query).fetchPolicy(FetchPolicy.CacheOnly).execute()
    assertNotNull(response.data)
  }
}
