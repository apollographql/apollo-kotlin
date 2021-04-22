package com.apollographql.apollo3.interceptor.cache

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.ApolloRequest
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.RequestContext
import com.apollographql.apollo3.api.ResponseContext
import com.apollographql.apollo3.cache.normalized.ApolloStore
import kotlinx.coroutines.flow.Flow

sealed class FetchPolicy {
  /**
   * Fetch the data from the normalized cache first. If it's not present in the
   * normalized cache or if an exception occurs while trying to fetch it from the normalized cache, then the data is
   * instead fetched from the network. If an exception happens while fetching from the network, this exception will
   * be thrown
   *
   * This is the default behaviour
   */
  object CacheFirst : FetchPolicy()

  /**
   * Only fetch the data from the normalized cache.
   */
  object CacheOnly : FetchPolicy()

  /**
   * Fetch the data from the network firsts. If network request fails, then the
   * data is fetched from the normalized cache. If the data is not present in the normalized cache, then the
   * exception which led to the network request failure is rethrown.
   */
  object NetworkFirst : FetchPolicy()

  /**
   * Only etch the GraphQL data from the network. If network request fails, an
   * exception is thrown.
   */
  object NetworkOnly : FetchPolicy()

  /**
   * Signal the apollo client to fetch the data from both the network and the cache. If cached data is not
   * present, only network data will be returned. If cached data is available, but network experiences an error,
   * cached data is first returned, followed by the network error. If cache data is not available, and network
   * data is not available, the error of the network request will be propagated. If both network and cache
   * are available, both will be returned. Cache data is guaranteed to be returned first.
   */
  object CacheAndNetwork : FetchPolicy()
}

internal data class CacheInput(
    val fetchPolicy: FetchPolicy,
    val watch: Boolean
): RequestContext(Key) {
  companion object Key : ExecutionContext.Key<CacheInput>
}

internal data class CacheOutput(
    val isFromCache: Boolean
) : ResponseContext(CacheOutput) {
  companion object Key : ExecutionContext.Key<CacheOutput>
}

val <D : Operation.Data> ApolloResponse<D>.isFromCache
  get() = executionContext[CacheOutput]?.isFromCache ?: false

fun ApolloClient.Builder.normalizedCache(store: ApolloStore): ApolloClient.Builder {
  return addInterceptor(
      ApolloCacheInterceptor(),
      store
  )
}

fun <D : Query.Data> ApolloClient.watch(queryRequest: ApolloRequest<D>): Flow<ApolloResponse<D>> {
  queryRequest.executionContext[CacheInput]
  return queryRequest.execute()
}
