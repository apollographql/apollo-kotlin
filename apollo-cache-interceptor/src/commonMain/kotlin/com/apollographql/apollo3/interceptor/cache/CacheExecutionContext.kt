package com.apollographql.apollo3.interceptor.cache

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.cache.normalized.NormalizedCache
import com.apollographql.apollo3.ApolloQueryRequest
import com.apollographql.apollo3.api.RequestContext
import com.apollographql.apollo3.api.ResponseContext

sealed class FetchPolicy : RequestContext(Key) {
  /**
   * Fetch the data from the normalized cache first. If it's not present in the
   * normalized cache or if an exception occurs while trying to fetch it from the normalized cache, then the data is
   * instead fetched from the network. If an exception happens while fetching from the network, this exception will
   * be thrown
   *
   * This is the default behaviour
   */
  object CACHE_FIRST : FetchPolicy()

  /**
   * Only fetch the data from the normalized cache.
   */
  object CACHE_ONLY : FetchPolicy()

  /**
   * Fetch the data from the network firsts. If network request fails, then the
   * data is fetched from the normalized cache. If the data is not present in the normalized cache, then the
   * exception which led to the network request failure is rethrown.
   */
  object NETWORK_FIRST : FetchPolicy()

  /**
   * Only etch the GraphQL data from the network. If network request fails, an
   * exception is thrown.
   */
  object NETWORK_ONLY : FetchPolicy()

  /**
   * Signal the apollo client to fetch the data from both the network and the cache. If cached data is not
   * present, only network data will be returned. If cached data is available, but network experiences an error,
   * cached data is first returned, followed by the network error. If cache data is not available, and network
   * data is not available, the error of the network request will be propagated. If both network and cache
   * are available, both will be returned. Cache data is guaranteed to be returned first.
   */
  object CACHE_AND_NETWORK : FetchPolicy()

  companion object Key : ExecutionContext.Key<FetchPolicy>
}

data class CacheInfo(
    val isFromCache: Boolean
) : ResponseContext(CacheInfo) {
  companion object Key : ExecutionContext.Key<CacheInfo>
}

fun <D : Operation.Data> ApolloQueryRequest.Builder<D>.fetchPolicy(policy: FetchPolicy) = apply {
  addExecutionContext(policy)
}

val <D : Operation.Data> ApolloResponse<D>.isFromCache
  get() = executionContext[CacheInfo]?.isFromCache ?: false


fun ApolloClient.Builder.normalizedCache(normalizedCache: NormalizedCache): ApolloClient.Builder {
  return addInterceptor(ApolloCacheInterceptor(), ApolloStore(normalizedCache))
}
