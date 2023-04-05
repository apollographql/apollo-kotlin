package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.Operation
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
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.testing.QueueTestNetworkTransport
import com.apollographql.apollo3.testing.enqueueTestResponse
import com.apollographql.apollo3.testing.internal.runTest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class CacheFlagsTest {
  private lateinit var apolloClient: ApolloClient
  private lateinit var store: ApolloStore

  private fun setUp() {
    store = ApolloStore(MemoryCacheFactory())
    apolloClient = ApolloClient.Builder().networkTransport(QueueTestNetworkTransport()).store(store).build()
  }

  @Test
  fun doNotStore() = runTest(before = { setUp() }) {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))
    apolloClient.enqueueTestResponse(query, data)

    apolloClient.query(query).doNotStore(true).execute()

    // Since the previous request was not stored, this should fail
    assertIs<CacheMissException>(
        apolloClient.query(query).fetchPolicy(FetchPolicy.CacheOnly).execute().exception
    )
  }

  @Test
  fun testEvictAfterRead() = runTest(before = { setUp() }) {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))
    apolloClient.enqueueTestResponse(query, data)

    // Store the data
    apolloClient.query(query).fetchPolicy(FetchPolicy.NetworkOnly).execute()

    // This should work and evict the entries
    val response = apolloClient.query(query)
        .fetchPolicy(FetchPolicy.CacheOnly)
        .cacheHeaders(CacheHeaders.builder().addHeader(ApolloCacheHeaders.EVICT_AFTER_READ, "true").build())
        .execute()

    assertEquals("R2-D2", response.data?.hero?.name)

    // Second time should fail
    assertIs<CacheMissException>(
        apolloClient.query(query).fetchPolicy(FetchPolicy.CacheOnly).execute().exception
    )
  }

  private val partialResponseData = HeroNameQuery.Data(null)
  private val partialResponseErrors = listOf(Error(
      message = "An error Happened",
      locations = listOf(Error.Location(0, 0)),
      path = null, extensions = null, nonStandardFields = null))


  @Test
  fun partialResponsesAreNotStored() = runTest(before = { setUp() }) {
    val query = HeroNameQuery()
    apolloClient.enqueueTestResponse(query, partialResponseData, partialResponseErrors)

    // this should not store the response
    apolloClient.query(query).execute()

    assertIs<CacheMissException>(
        apolloClient.query(query).fetchPolicy(FetchPolicy.CacheOnly).execute().exception
    )
  }

  @Test
  fun storePartialResponse() = runTest(before = { setUp() }) {
    val query = HeroNameQuery()
    apolloClient.enqueueTestResponse(query, partialResponseData, partialResponseErrors)

    // this should store the response
    apolloClient.query(query).storePartialResponses(true).execute()

    val response = apolloClient.query(query).fetchPolicy(FetchPolicy.CacheOnly).execute()
    assertNotNull(response.data)
  }

  @Test
  fun doNotStoreWhenSetInResponse() = runTest(before = { setUp() }) {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))
    apolloClient = apolloClient.newBuilder().addInterceptor(object: ApolloInterceptor{
      override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
        return chain.proceed(request).map { response ->
          response.newBuilder().cacheHeaders(CacheHeaders.Builder().addHeader(ApolloCacheHeaders.DO_NOT_STORE, "").build()).build()
        }
      }
    }).build()
    apolloClient.enqueueTestResponse(query, data)

    apolloClient.query(query).fetchPolicy(FetchPolicy.NetworkFirst).execute()

    // Since the previous request was not stored, this should fail
    assertIs<CacheMissException>(
        apolloClient.query(query).fetchPolicy(FetchPolicy.CacheOnly).execute().exception
    )
  }
}
