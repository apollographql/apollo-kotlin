package com.apollographql.apollo3.cache.normalized

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ClientContext
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.exception.ApolloCompositeException
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.internal.ApolloCacheInterceptor
import com.apollographql.apollo3.cache.normalized.internal.CacheInput
import com.apollographql.apollo3.cache.normalized.internal.CacheOutput
import com.apollographql.apollo3.cache.normalized.internal.DefaultCacheInput
import com.apollographql.apollo3.cache.normalized.internal.StoreExecutionContext
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

/**
 * Configures an [ApolloClient] with a normalized cache.
 *
 * @param normalizedCacheFactory a factory that creates a [NormalizedCache]. It will only be called once.
 * The reason this is a factory is to enforce creating the cache from a non-main thread. For native the thread
 * where the cache is created will also be isolated so that the cache can be mutated.
 *
 * @param cacheResolver a [CacheResolver] to customize normalization
 *
 * @param writeToCacheAsynchronously set to true to write to the cache after the response has been emitted.
 * This allows to display results faster
 */
fun ApolloClient.withNormalizedCache(
    normalizedCacheFactory: NormalizedCacheFactory,
    cacheResolver: CacheResolver = CacheResolver(),
    writeToCacheAsynchronously: Boolean = false
): ApolloClient {
  return withStore(ApolloStore(normalizedCacheFactory, cacheResolver), writeToCacheAsynchronously)
}

fun ApolloClient.withStore(store: ApolloStore, writeToCacheAsynchronously: Boolean = false): ApolloClient {
  return withInterceptor(ApolloCacheInterceptor(store, writeToCacheAsynchronously))
      .withExecutionContext(StoreExecutionContext(store))
}

val ApolloClient.store: ApolloStore
  get() = executionContext[StoreExecutionContext]?.store ?: error("This ApolloClient doesn't have a store")


fun <D: Query.Data> ApolloRequest<D>.withFetchPolicy(fetchPolicy: FetchPolicy): ApolloRequest<D> {
  val context = executionContext[CacheInput] ?: DefaultCacheInput(operation)
  return withExecutionContext(context.copy(fetchPolicy = fetchPolicy))
}
fun <D: Query.Data> ApolloRequest<D>.withRefetchPolicy(refetchPolicy: FetchPolicy): ApolloRequest<D> {
  val context = executionContext[CacheInput] ?: DefaultCacheInput(operation)
  return withExecutionContext(context.copy(refetchPolicy = refetchPolicy))
}
fun <D: Operation.Data> ApolloRequest<D>.withCacheFlags(flags: Int): ApolloRequest<D> {
  val context = executionContext[CacheInput] ?: DefaultCacheInput(operation)
  return withExecutionContext(context.copy(flags = flags))
}
fun <D: Operation.Data> ApolloRequest<D>.withCacheHeaders(cacheHeaders: CacheHeaders): ApolloRequest<D> {
  val context = executionContext[CacheInput] ?: DefaultCacheInput(operation)
  return withExecutionContext(context.copy(cacheHeaders = cacheHeaders))
}
fun <D: Mutation.Data> ApolloRequest<D>.withOptimisticUpdates(data: D): ApolloRequest<D> {
  val context = executionContext[CacheInput] ?: DefaultCacheInput(operation)
  return withExecutionContext(context.copy(optimisticData = data))
}

fun <D: Operation.Data> ApolloRequest<D>.withCacheContext(
    fetchPolicy: FetchPolicy,
    refetchPolicy: FetchPolicy? = null,
    data: D? = null,
    flags: Int = 0
): ApolloRequest<D> {
  return withExecutionContext(CacheInput(fetchPolicy, refetchPolicy, data, flags))
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
  var context = queryRequest.executionContext[CacheInput]
  if (context == null) {
    context = CacheInput(FetchPolicy.CacheFirst, FetchPolicy.CacheOnly)
  } else if (context.refetchPolicy == null) {
    context = context.copy(refetchPolicy = FetchPolicy.CacheOnly)
  }

  return queryAsFlow(queryRequest.withExecutionContext(context))
}

val <D : Operation.Data> ApolloResponse<D>.isFromCache
  get() = executionContext[CacheOutput]?.isFromCache ?: false


