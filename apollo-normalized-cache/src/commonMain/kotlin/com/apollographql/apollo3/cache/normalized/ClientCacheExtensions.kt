package com.apollographql.apollo3.cache.normalized

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.HasMutableExecutionContext
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.internal.ApolloCacheInterceptor
import com.apollographql.apollo3.exception.ApolloCompositeException
import com.apollographql.apollo3.exception.ApolloException
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
fun ApolloClient.Builder.normalizedCache(
    normalizedCacheFactory: NormalizedCacheFactory,
    objectIdGenerator: ObjectIdGenerator = TypePolicyObjectIdGenerator,
    cacheResolver: CacheResolver = FieldPolicyCacheResolver,
    writeToCacheAsynchronously: Boolean = false,
): ApolloClient.Builder {
  return store(ApolloStore(normalizedCacheFactory, objectIdGenerator, cacheResolver), writeToCacheAsynchronously)
}

fun ApolloClient.Builder.store(store: ApolloStore, writeToCacheAsynchronously: Boolean = false): ApolloClient.Builder {
  return addInterceptor(ApolloCacheInterceptor(store)).withWriteToCacheAsynchronously(writeToCacheAsynchronously)
}

fun <D : Query.Data> ApolloClient.watch(query: Query<D>): Flow<ApolloResponse<D>> {
  return watch(ApolloRequest.Builder(query).build())
}

fun <D : Query.Data> ApolloClient.watch(queryRequest: ApolloRequest<D>): Flow<ApolloResponse<D>> {
  return queryAsFlow(queryRequest.newBuilder().withExecutionContext(WatchContext(true)).build())
}

fun <D : Query.Data> ApolloClient.queryCacheAndNetwork(query: Query<D>): Flow<ApolloResponse<D>> {
  return queryCacheAndNetwork(ApolloRequest.Builder(query).build())
}


/**
 * Gets the result from the cache first and always fetch from the network. Use this to get an early
 * cached result while also updating the network values.
 *
 * Any [FetchPolicy] on [queryRequest] will be ignored
 */
fun <D : Query.Data> ApolloClient.queryCacheAndNetwork(queryRequest: ApolloRequest<D>): Flow<ApolloResponse<D>> {
  return flow {
    var cacheException: ApolloException? = null
    var networkException: ApolloException? = null
    try {
     emit(query(queryRequest.newBuilder().withFetchPolicy(FetchPolicy.CacheOnly).build()))
    } catch (e: ApolloException) {
      cacheException = e
    }

    try {
      emit(query(queryRequest.newBuilder().withFetchPolicy(FetchPolicy.NetworkOnly).build()))
    } catch (e: ApolloException) {
      networkException = e
    }

    if (cacheException != null && networkException != null) {
      throw ApolloCompositeException(
          cacheException,
          networkException
      )
    }
  }
}

val ApolloClient.apolloStore: ApolloStore
  get() {
    return interceptors.firstOrNull { it is ApolloCacheInterceptor }?.let {
      (it as ApolloCacheInterceptor).store
    } ?: error("no cache configured")
  }

@Deprecated(
    message = "Use apolloStore directly",
    replaceWith = ReplaceWith("apolloStore.clearAll()")
)
fun ApolloClient.clearNormalizedCache() = apolloStore.clearAll()

/**
 * Sets the [FetchPolicy] on this request. D has a bound on [Query.Data] because subscriptions and mutation shouldn't
 * read the cache
 */
fun <D : Query.Data> ApolloRequest.Builder<D>.withFetchPolicy(fetchPolicy: FetchPolicy) = withExecutionContext(
    FetchPolicyContext(fetchPolicy)
)

/**
 * Sets the default [FetchPolicy] for the [ApolloClient]. This only affects queries. Mutations and subscriptions will
 * always use [FetchPolicy.NetworkFirst]
 */
fun ApolloClient.Builder.withFetchPolicy(fetchPolicy: FetchPolicy) = withExecutionContext(
    FetchPolicyContext(fetchPolicy)
)

/**
 * Sets the [FetchPolicy] used when refetching at the request level. This is only used in combination with [watch].
 */
fun <D : Query.Data> ApolloRequest.Builder<D>.withRefetchPolicy(refetchPolicy: FetchPolicy) = withExecutionContext(
    RefetchPolicyContext(refetchPolicy)
)

/**
 * Sets the [FetchPolicy] used when refetching at the client level. This is only used in combination with [watch].
 */
fun ApolloClient.Builder.withRefetchPolicy(refetchPolicy: FetchPolicy) = withExecutionContext(
    RefetchPolicyContext(refetchPolicy)
)

fun <T> HasMutableExecutionContext<T>.withDoNotStore(doNotStore: Boolean) where T : HasMutableExecutionContext<T> = withExecutionContext(
    DoNotStoreContext(doNotStore)
)

fun <T> HasMutableExecutionContext<T>.withStorePartialResponses(storePartialResponses: Boolean) where T : HasMutableExecutionContext<T> = withExecutionContext(
    StorePartialResponsesContext(storePartialResponses)
)

fun <T> HasMutableExecutionContext<T>.withCacheHeaders(cacheHeaders: CacheHeaders) where T : HasMutableExecutionContext<T> = withExecutionContext(
    CacheHeadersContext(cacheHeaders)
)

fun <T> HasMutableExecutionContext<T>.withWriteToCacheAsynchronously(writeToCacheAsynchronously: Boolean) where T : HasMutableExecutionContext<T> = withExecutionContext(
    WriteToCacheAsynchronouslyContext(writeToCacheAsynchronously)
)

/**
 * Sets the optimistic updates to write to the cache while a query is pending.
 */
fun <D : Mutation.Data> ApolloRequest.Builder<D>.withOptimisticUpdates(data: D) = withExecutionContext(
    OptimisticUpdatesContext(data)
)

internal val <D : Query.Data> ApolloRequest<D>.fetchPolicy
  get() = executionContext[FetchPolicyContext]?.value ?: FetchPolicy.CacheFirst

internal val <D : Query.Data> ApolloRequest<D>.refetchPolicy
  get() = executionContext[RefetchPolicyContext]?.value ?: FetchPolicy.CacheOnly

internal val <D : Operation.Data> ApolloRequest<D>.doNotStore
  get() = executionContext[DoNotStoreContext]?.value ?: false

internal val <D : Operation.Data> ApolloRequest<D>.storePartialResponses
  get() = executionContext[StorePartialResponsesContext]?.value ?: false

internal val <D : Operation.Data> ApolloRequest<D>.writeToCacheAsynchronously
  get() = executionContext[WriteToCacheAsynchronouslyContext]?.value ?: false

internal val <D : Mutation.Data> ApolloRequest<D>.optimisticData
  get() = executionContext[OptimisticUpdatesContext]?.value

internal val <D : Operation.Data> ApolloRequest<D>.cacheHeaders
  get() = executionContext[CacheHeadersContext]?.value ?: CacheHeaders.NONE

internal val <D : Operation.Data> ApolloRequest<D>.watch
  get() = executionContext[WatchContext]?.value ?: false


class CacheInfo(
    val millisStart: Long,
    val millisEnd: Long,
    val hit: Boolean,
    val missedKey: String?,
    val missedField: String?,
) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<CacheInfo>
}

val <D : Operation.Data> ApolloResponse<D>.isFromCache
  get() = cacheInfo?.hit ?: false

val <D : Operation.Data> ApolloResponse<D>.cacheInfo
  get() = executionContext[CacheInfo]

internal fun <D : Operation.Data> ApolloResponse<D>.withCacheInfo(cacheInfo: CacheInfo) = withExecutionContext(cacheInfo)

internal class FetchPolicyContext(val value: FetchPolicy) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<FetchPolicyContext>
}

internal class RefetchPolicyContext(val value: FetchPolicy) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<RefetchPolicyContext>
}

internal class DoNotStoreContext(val value: Boolean) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<DoNotStoreContext>
}

internal class StorePartialResponsesContext(val value: Boolean) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<StorePartialResponsesContext>
}

internal class WriteToCacheAsynchronouslyContext(val value: Boolean) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<WriteToCacheAsynchronouslyContext>
}

internal class CacheHeadersContext(val value: CacheHeaders) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<CacheHeadersContext>
}

internal class OptimisticUpdatesContext<D : Mutation.Data>(val value: D) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<OptimisticUpdatesContext<*>>
}

internal class WatchContext(val value: Boolean) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<WatchContext>
}
