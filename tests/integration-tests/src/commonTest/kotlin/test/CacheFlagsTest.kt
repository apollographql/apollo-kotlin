package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.api.ApolloCacheHeaders
import com.apollographql.apollo.cache.normalized.api.CacheHeaders
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.cacheHeaders
import com.apollographql.apollo.cache.normalized.doNotStore
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.store
import com.apollographql.apollo.cache.normalized.storePartialResponses
import com.apollographql.apollo.exception.CacheMissException
import com.apollographql.apollo.integration.normalizer.HeroNameQuery
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.apollo.testing.QueueTestNetworkTransport
import com.apollographql.apollo.testing.enqueueTestResponse
import com.apollographql.apollo.testing.internal.runTest
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
  private val partialResponseErrors = listOf(
      Error.Builder(message = "An error Happened")
          .locations(listOf(Error.Location(0, 0)))
          .build()
  )


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
