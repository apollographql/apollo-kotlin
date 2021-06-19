package com.apollographql.apollo3.interceptor.cache

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.exception.ApolloCompositeException
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.interceptor.cache.internal.ApolloCacheInterceptor
import com.apollographql.apollo3.interceptor.cache.internal.CacheContext
import com.apollographql.apollo3.interceptor.cache.internal.CacheOutput
import com.apollographql.apollo3.interceptor.cache.internal.DefaultCacheContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

enum class FetchPolicy {
  /**
   * Try cache first, then network
   *
   * This is the default behaviour
   */
  CacheFirst,

  /**
   * Only try cache
   */
  CacheOnly,

  /**
   * Try network first, then cache
   */
  NetworkFirst,

  /**
   * Only try network
   */
  NetworkOnly,
}

const val CACHE_FLAG_DO_NOT_STORE = 1
const val CACHE_FLAG_STORE_PARTIAL_RESPONSE = 2
const val CACHE_FLAG_EVICT_AFTER_READ = 4

fun ApolloClient.withStore(store: ApolloStore): ApolloClient {
  return withInterceptor(ApolloCacheInterceptor(store))
}

fun <D: Query.Data> ApolloRequest<D>.withFetchPolicy(fetchPolicy: FetchPolicy): ApolloRequest<D> {
  val context = executionContext[CacheContext] ?: DefaultCacheContext(operation)
  return withExecutionContext(context.copy(fetchPolicy = fetchPolicy))
}
fun <D: Query.Data> ApolloRequest<D>.withRefetchPolicy(refetchPolicy: FetchPolicy): ApolloRequest<D> {
  val context = executionContext[CacheContext] ?: DefaultCacheContext(operation)
  return withExecutionContext(context.copy(fetchPolicy = refetchPolicy))
}
fun <D: Operation.Data> ApolloRequest<D>.withCacheFlags(flags: Int): ApolloRequest<D> {
  val context = executionContext[CacheContext] ?: DefaultCacheContext(operation)
  return withExecutionContext(context.copy(flags = flags))
}
fun <D: Mutation.Data> ApolloRequest<D>.withOptimiticUpdates(data: D): ApolloRequest<D> {
  val context = executionContext[CacheContext] ?: DefaultCacheContext(operation)
  return withExecutionContext(context.copy(optimisticData = data))
}

fun <D: Operation.Data> ApolloRequest<D>.withCacheContext(
    fetchPolicy: FetchPolicy,
    refetchPolicy: FetchPolicy? = null,
    data: D? = null,
    flags: Int = 0
): ApolloRequest<D> {
  return withExecutionContext(CacheContext(fetchPolicy, refetchPolicy, data, flags))
}


/**
 * Gets the result from the cache first and always fetch from the network. Use this to get an early
 * cached result while also updating the network values.
 *
 * Any [FetchPolicy] on [queryRequest] will be ignored
 */
fun <D : Query.Data> ApolloClient.queryCacheAndNetwork(queryRequest: ApolloRequest<D>): Flow<ApolloResponse<D>> {
  return flow {
    val cacheResult = kotlin.runCatching {
      query(queryRequest.withFetchPolicy(FetchPolicy.CacheOnly))
    }
    val cacheResponse = cacheResult.getOrNull()
    if (cacheResponse != null) {
      emit(cacheResponse)
    }
    val networkResult = kotlin.runCatching {
      query(queryRequest.withFetchPolicy(FetchPolicy.NetworkOnly))
    }
    val networkResponse = networkResult.getOrNull()
    if (networkResponse != null) {
      emit(networkResponse)
    }

    if (cacheResponse == null && networkResponse == null) {
      throw ApolloCompositeException(
          cacheResult.exceptionOrNull(),
          networkResult.exceptionOrNull()
      )
    }
  }
}

fun <D : Query.Data> ApolloClient.queryCacheAndNetwork(query: Query<D>): Flow<ApolloResponse<D>> {
  return queryCacheAndNetwork(ApolloRequest(query))
}

fun <D : Query.Data> ApolloClient.watch(query: Query<D>): Flow<ApolloResponse<D>> {
  return watch(ApolloRequest(query))
}

fun <D : Query.Data> ApolloClient.watch(queryRequest: ApolloRequest<D>): Flow<ApolloResponse<D>> {
  var context = queryRequest.executionContext[CacheContext]
  if (context == null) {
    context = CacheContext(FetchPolicy.CacheFirst, FetchPolicy.CacheOnly)
  } else if (context.refetchPolicy == null) {
    context = context.copy(refetchPolicy = FetchPolicy.CacheOnly)
  }

  return queryAsFlow(queryRequest.withExecutionContext(context))
}

val <D : Operation.Data> ApolloResponse<D>.isFromCache
  get() = executionContext[CacheOutput]?.isFromCache ?: false


