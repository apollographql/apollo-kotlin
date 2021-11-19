@file:JvmName("NormalizedCache")

package com.apollographql.apollo3.cache.normalized

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.ApolloMutationCall
import com.apollographql.apollo3.ApolloQueryCall
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.HasMutableExecutionContext
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.CacheResolver
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyCacheResolver
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.internal.ApolloCacheInterceptor
import com.apollographql.apollo3.exception.ApolloCompositeException
import com.apollographql.apollo3.exception.ApolloException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

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
@JvmOverloads
@JvmName("configureApolloClientBuilder")
fun ApolloClient.Builder.normalizedCache(
    normalizedCacheFactory: NormalizedCacheFactory,
    cacheKeyGenerator: CacheKeyGenerator = TypePolicyCacheKeyGenerator,
    cacheResolver: CacheResolver = FieldPolicyCacheResolver,
    writeToCacheAsynchronously: Boolean = false,
): ApolloClient.Builder {
  return store(ApolloStore(normalizedCacheFactory, cacheKeyGenerator, cacheResolver), writeToCacheAsynchronously)
}

fun ApolloClient.Builder.store(store: ApolloStore, writeToCacheAsynchronously: Boolean = false): ApolloClient.Builder {
  return addInterceptor(ApolloCacheInterceptor(store)).writeToCacheAsynchronously(writeToCacheAsynchronously)
}

/***
 * Gets the result from the network, then observes the cache for any changes.
 * Overriding the [FetchPolicy] will change how the result is first queried.
 */
fun <D : Query.Data> ApolloQueryCall<D>.watch(): Flow<ApolloResponse<D>> {
  return copy().addExecutionContext(WatchContext(true)).executeAsFlow()
}

/**
 * Gets the result from the cache first and always fetch from the network. Use this to get an early
 * cached result while also updating the network values.
 *
 * Any [FetchPolicy] on [queryRequest] will be ignored
 */
fun <D : Query.Data> ApolloQueryCall<D>.executeCacheAndNetwork(): Flow<ApolloResponse<D>> {
  return flow {
    var cacheException: ApolloException? = null
    var networkException: ApolloException? = null
    try {
      emit(copy().fetchPolicy(FetchPolicy.CacheOnly).execute())
    } catch (e: ApolloException) {
      cacheException = e
    }

    try {
      emit(copy().fetchPolicy(FetchPolicy.NetworkOnly).execute())
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

@Deprecated("Used for backward compatibility with 2.x.", ReplaceWith("apolloStore"))
fun ApolloClient.apolloStore(): ApolloStore = apolloStore

@Deprecated(
    message = "Use apolloStore directly",
    replaceWith = ReplaceWith("apolloStore.clearAll()")
)
fun ApolloClient.clearNormalizedCache() = apolloStore.clearAll()

/**
 * Sets the initial [FetchPolicy]
 * This only has effects for queries. Mutations and subscriptions always use [FetchPolicy.NetworkOnly]
 */
fun <T : HasMutableExecutionContext<T>> HasMutableExecutionContext<T>.fetchPolicy(fetchPolicy: FetchPolicy) = addExecutionContext(
    FetchPolicyContext(fetchPolicy)
)

/**
 * Sets the [FetchPolicy] used when watching queries and a cache change has been published
 */
fun <T : HasMutableExecutionContext<T>> HasMutableExecutionContext<T>.refetchPolicy(fetchPolicy: FetchPolicy) = addExecutionContext(
    RefetchPolicyContext(fetchPolicy)
)

fun <T : HasMutableExecutionContext<T>> HasMutableExecutionContext<T>.doNotStore(doNotStore: Boolean) = addExecutionContext(
    DoNotStoreContext(doNotStore)
)

fun <T : HasMutableExecutionContext<T>> HasMutableExecutionContext<T>.storePartialResponses(storePartialResponses: Boolean) = addExecutionContext(
    StorePartialResponsesContext(storePartialResponses)
)

fun <T : HasMutableExecutionContext<T>> HasMutableExecutionContext<T>.cacheHeaders(cacheHeaders: CacheHeaders) = addExecutionContext(
    CacheHeadersContext(cacheHeaders)
)

fun <T : HasMutableExecutionContext<T>> HasMutableExecutionContext<T>.writeToCacheAsynchronously(writeToCacheAsynchronously: Boolean) = addExecutionContext(
    WriteToCacheAsynchronouslyContext(writeToCacheAsynchronously)
)

/**
 * Sets the optimistic updates to write to the cache while a query is pending.
 */
fun <D : Mutation.Data> ApolloRequest.Builder<D>.optimisticUpdates(data: D) = addExecutionContext(
    OptimisticUpdatesContext(data)
)

fun <D : Mutation.Data> ApolloMutationCall<D>.optimisticUpdates(data: D) = addExecutionContext(
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

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withNormalizedCache(
    normalizedCacheFactory: NormalizedCacheFactory,
    cacheKeyGenerator: CacheKeyGenerator = TypePolicyCacheKeyGenerator,
    cacheResolver: CacheResolver = FieldPolicyCacheResolver,
    writeToCacheAsynchronously: Boolean = false,
) = newBuilder().normalizedCache(
    normalizedCacheFactory = normalizedCacheFactory,
    cacheKeyGenerator = cacheKeyGenerator,
    cacheResolver = cacheResolver,
    writeToCacheAsynchronously = writeToCacheAsynchronously,
)
    .build()

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withStore(
    store: ApolloStore,
    writeToCacheAsynchronously: Boolean = false,
) = newBuilder().store(store, writeToCacheAsynchronously).build()

@Deprecated("Please use ApolloRequest.Builder methods instead. This will be removed in v3.0.0.")
fun <D : Query.Data> ApolloRequest<D>.withFetchPolicy(fetchPolicy: FetchPolicy) = newBuilder().fetchPolicy(fetchPolicy).build()

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withFetchPolicy(fetchPolicy: FetchPolicy) = newBuilder().fetchPolicy(fetchPolicy).build()

@Deprecated("Please use ApolloRequest.Builder methods instead. This will be removed in v3.0.0.")
fun <D : Query.Data> ApolloRequest<D>.withRefetchPolicy(refetchPolicy: FetchPolicy) = newBuilder().refetchPolicy(refetchPolicy).build()

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withRefetchPolicy(refetchPolicy: FetchPolicy) = newBuilder().refetchPolicy(refetchPolicy).build()

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withDoNotStore(doNotStore: Boolean) = newBuilder().doNotStore(doNotStore).build()

@Deprecated("Please use ApolloRequest.Builder methods instead. This will be removed in v3.0.0.")
fun <D : Operation.Data> ApolloRequest<D>.withDoNotStore(doNotStore: Boolean) = newBuilder().doNotStore(doNotStore).build()

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withStorePartialResponses(storePartialResponses: Boolean) = newBuilder().storePartialResponses(storePartialResponses).build()

@Deprecated("Please use ApolloRequest.Builder methods instead. This will be removed in v3.0.0.")
fun <D : Operation.Data> ApolloRequest<D>.withStorePartialResponses(storePartialResponses: Boolean) = newBuilder().storePartialResponses(storePartialResponses).build()

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withCacheHeaders(cacheHeaders: CacheHeaders) = newBuilder().cacheHeaders(cacheHeaders).build()

@Deprecated("Please use ApolloRequest.Builder methods instead. This will be removed in v3.0.0.")
fun <D : Operation.Data> ApolloRequest<D>.withCacheHeaders(cacheHeaders: CacheHeaders) = newBuilder().cacheHeaders(cacheHeaders).build()

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withWriteToCacheAsynchronously(writeToCacheAsynchronously: Boolean) = newBuilder().writeToCacheAsynchronously(writeToCacheAsynchronously).build()

@Deprecated("Please use ApolloRequest.Builder methods instead. This will be removed in v3.0.0.")
fun <D : Operation.Data> ApolloRequest<D>.withWriteToCacheAsynchronously(writeToCacheAsynchronously: Boolean) = newBuilder().writeToCacheAsynchronously(writeToCacheAsynchronously).build()

@Deprecated("Please use ApolloRequest.Builder methods instead. This will be removed in v3.0.0.")
fun <D : Mutation.Data> ApolloRequest<D>.withOptimisticUpdates(data: D) = newBuilder().optimisticUpdates(data).build()
