package com.apollographql.apollo.interceptor.cache

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.cache.normalized.NormalizedCache
import com.apollographql.apollo.ApolloQueryRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ApolloExperimental
data class CacheResponseExecutionContext(
    val fromCache: Boolean
) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*> = Key

  companion object Key : ExecutionContext.Key<CacheResponseExecutionContext>
}

enum class FetchPolicy {
  /**
   * Fetch the data from the normalized cache first. If it's not present in the
   * normalized cache or if an exception occurs while trying to fetch it from the normalized cache, then the data is
   * instead fetched from the network. If an exception happens while fetching from the network, this exception will
   * be thrown
   *
   * This is the default behaviour
   */
  CACHE_FIRST,
  /**
   * Only fetch the data from the normalized cache.
   */
  CACHE_ONLY,
  /**
   * Fetch the data from the network firsts. If network request fails, then the
   * data is fetched from the normalized cache. If the data is not present in the normalized cache, then the
   * exception which led to the network request failure is rethrown.
   */
  NETWORK_FIRST,
  /**
   * Only etch the GraphQL data from the network. If network request fails, an
   * exception is thrown.
   */
  NETWORK_ONLY,
  /**
   * Signal the apollo client to fetch the data from both the network and the cache. If cached data is not
   * present, only network data will be returned. If cached data is available, but network experiences an error,
   * cached data is first returned, followed by the network error. If cache data is not available, and network
   * data is not available, the error of the network request will be propagated. If both network and cache
   * are available, both will be returned. Cache data is guaranteed to be returned first.
   */
  CACHE_AND_NETWORK
}

@ApolloExperimental
data class CacheRequestExecutionContext(
    val policy: FetchPolicy
) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*> = Key

  companion object Key : ExecutionContext.Key<CacheRequestExecutionContext>
}

@ApolloExperimental
fun <D : Operation.Data> ApolloQueryRequest.Builder<D>.fetchPolicy(policy: FetchPolicy) = apply {
  addExecutionContext(CacheRequestExecutionContext(policy))
}

@ApolloExperimental
val <D : Operation.Data> Response<D>.fromCache
  get() = executionContext[CacheResponseExecutionContext]?.fromCache ?: throw IllegalStateException("ApolloGraphQL: no CacheExecutionContext")


@ExperimentalCoroutinesApi
@ApolloExperimental
fun ApolloClient.Builder.normalizedCache(normalizedCache: NormalizedCache): ApolloClient.Builder {
  return addInterceptor(ApolloCacheInterceptor(ApolloStore(normalizedCache)))
}
