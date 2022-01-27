@file:JvmName("NormalizedCache")

package com.apollographql.apollo3.cache.normalized

import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_0_0
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.MutableExecutionOptions
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
import com.apollographql.apollo3.cache.normalized.internal.WatcherInterceptor
import com.apollographql.apollo3.exception.ApolloCompositeException
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.CacheMissException
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
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
 * @param normalizedCacheFactory a factory that creates a [com.apollographql.apollo3.cache.normalized.api.NormalizedCache].
 * It will only be called once.
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

@JvmName("-logCacheMisses")
fun ApolloClient.Builder.logCacheMisses(
    log: (String) -> Unit = { println(it) },
): ApolloClient.Builder {
  check(interceptors.none { it is ApolloCacheInterceptor }) {
    "Apollo: logCacheMisses() must be called before setting up your normalized cache"
  }
  return addInterceptor(CacheMissLoggingInterceptor(log))
}

fun ApolloClient.Builder.store(store: ApolloStore, writeToCacheAsynchronously: Boolean = false): ApolloClient.Builder {
  return addInterceptor(WatcherInterceptor(store))
      .addInterceptor(FetchPolicyRouterInterceptor)
      .addInterceptor(ApolloCacheInterceptor(store))
      .writeToCacheAsynchronously(writeToCacheAsynchronously)
}

enum class WatchErrorHandling {
  THROW_CACHE_ERRORS,
  THROW_NETWORK_ERRORS,
  THROW_CACHE_AND_NETWORK_ERRORS,
  IGNORE_ERRORS
}

/***
 * Gets the result from the network, then observes the cache for any changes.
 * Overriding the [FetchPolicy] will change how the result is first queried.
 * Exception are ignored by default, this can be changed by setting [fetchErrorHandling] for the first fetch and [refetchErrorHandling]
 * for subsequent fetches.
 */
fun <D : Query.Data> ApolloCall<D>.watch(
    fetchErrorHandling: WatchErrorHandling = WatchErrorHandling.IGNORE_ERRORS,
    refetchErrorHandling: WatchErrorHandling = WatchErrorHandling.IGNORE_ERRORS,
): Flow<ApolloResponse<D>> {
  var data: D? = null
  return toFlow()
      .catch {
        maybeThrow(it, fetchErrorHandling)
      }.onEach {
        data = it.data
      }.onCompletion {
        emitAll(
            watch(data)
                .catch {
                  maybeThrow(it, refetchErrorHandling)
                }
        )
      }
}

/**
 * Observes the cache for the given data. Unlike [watch], no initial request is executed on the network.
 * The refetch policy set by [refetchPolicy] will be used.
 * Cache and network exceptions are propagated.
 */
fun <D : Query.Data> ApolloCall<D>.watch(data: D?): Flow<ApolloResponse<D>> {
  return copy().addExecutionContext(WatchContext(data)).toFlow()
}

private fun maybeThrow(throwable: Throwable, errorHandling: WatchErrorHandling) {
  // Only potentially swallow Apollo exceptions, the rest must be surfaced
  if (throwable !is ApolloException) {
    throw throwable
  }
  if (errorHandling == WatchErrorHandling.IGNORE_ERRORS) return
  val throwCacheErrors = errorHandling == WatchErrorHandling.THROW_CACHE_AND_NETWORK_ERRORS || errorHandling == WatchErrorHandling.THROW_CACHE_ERRORS
  val throwNetworkErrors = errorHandling == WatchErrorHandling.THROW_CACHE_AND_NETWORK_ERRORS || errorHandling == WatchErrorHandling.THROW_NETWORK_ERRORS
  when (throwable) {
    is ApolloCompositeException -> {
      if (errorHandling == WatchErrorHandling.THROW_CACHE_AND_NETWORK_ERRORS) {
        throw throwable
      }
      val cacheMissException = throwable.first as? CacheMissException ?: throwable.second as? CacheMissException
      // If it's *not* a CacheMissException we consider it a network error (could be ApolloNetworkException, ApolloHttpException, ApolloParseException...)
      val networkException = throwable.first.takeIf { it !is CacheMissException } ?: throwable.second.takeIf { it !is CacheMissException }
      if (cacheMissException != null && throwCacheErrors) {
        throw cacheMissException
      }
      if (networkException != null && throwNetworkErrors) {
        throw networkException
      }
    }
    is CacheMissException -> if (throwCacheErrors) {
      throw throwable
    }
    // Treat all other exceptions as network errors (could be ApolloNetworkException, ApolloHttpException, ApolloParseException...)
    else -> if (throwNetworkErrors) {
      throw throwable
    }
  }
}


/**
 * Gets the result from the cache first and always fetch from the network. Use this to get an early
 * cached result while also updating the network values.
 *
 * Any [FetchPolicy] previously set will be ignored
 */
fun <D : Query.Data> ApolloCall<D>.executeCacheAndNetwork(): Flow<ApolloResponse<D>> {
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
@ApolloDeprecatedSince(v3_0_0)
fun ApolloClient.apolloStore(): ApolloStore = apolloStore

@Deprecated(
    message = "Use apolloStore directly",
    replaceWith = ReplaceWith("apolloStore.clearAll()")
)
@ApolloDeprecatedSince(v3_0_0)
fun ApolloClient.clearNormalizedCache() = apolloStore.clearAll()

/**
 * Sets the initial [FetchPolicy]
 * This only has effects for queries. Mutations and subscriptions always use [FetchPolicy.NetworkOnly]
 */
fun <T> MutableExecutionOptions<T>.fetchPolicy(fetchPolicy: FetchPolicy) = addExecutionContext(
    FetchPolicyContext(interceptorFor(fetchPolicy))
)

/**
 * Sets the [FetchPolicy] used when watching queries and a cache change has been published
 */
fun <T> MutableExecutionOptions<T>.refetchPolicy(fetchPolicy: FetchPolicy) = addExecutionContext(
    RefetchPolicyContext(interceptorFor(fetchPolicy))
)

/**
 * Sets the initial [FetchPolicy]
 * This only has effects for queries. Mutations and subscriptions always use [FetchPolicy.NetworkOnly]
 */
fun <T> MutableExecutionOptions<T>.fetchPolicyInterceptor(interceptor: ApolloInterceptor) = addExecutionContext(
    FetchPolicyContext(interceptor)
)

/**
 * Sets the [FetchPolicy] used when watching queries and a cache change has been published
 */
fun <T> MutableExecutionOptions<T>.refetchPolicyInterceptor(interceptor: ApolloInterceptor) = addExecutionContext(
    RefetchPolicyContext(interceptor)
)

private fun interceptorFor(fetchPolicy: FetchPolicy) = when (fetchPolicy) {
  FetchPolicy.CacheOnly -> CacheOnlyInterceptor
  FetchPolicy.NetworkOnly -> NetworkOnlyInterceptor
  FetchPolicy.CacheFirst -> CacheFirstInterceptor
  FetchPolicy.NetworkFirst -> NetworkFirstInterceptor
}

/**
 * @param doNotStore Whether to store the response in cache.
 *
 * Default: false
 */
fun <T> MutableExecutionOptions<T>.doNotStore(doNotStore: Boolean) = addExecutionContext(
    DoNotStoreContext(doNotStore)
)

/**
 * @param storePartialResponses Whether to store partial responses.
 *
 * Errors are not stored in the cache and are therefore not replayed on cache reads.
 * Set this to true if you want to store partial responses at the risk of also returning partial responses
 * in subsequent cache reads.
 *
 * Default: false
 */
fun <T> MutableExecutionOptions<T>.storePartialResponses(storePartialResponses: Boolean) = addExecutionContext(
    StorePartialResponsesContext(storePartialResponses)
)

/**
 * @param cacheHeaders additional cache headers to be passed to your [com.apollographql.apollo3.cache.normalized.api.NormalizedCache]
 */
fun <T> MutableExecutionOptions<T>.cacheHeaders(cacheHeaders: CacheHeaders) = addExecutionContext(
    CacheHeadersContext(cacheHeaders)
)

/**
 * @param writeToCacheAsynchronously whether to return the response before writing it to the cache
 *
 * Setting this to true reduces the latency
 *
 * Default: false
 */
fun <T> MutableExecutionOptions<T>.writeToCacheAsynchronously(writeToCacheAsynchronously: Boolean) = addExecutionContext(
    WriteToCacheAsynchronouslyContext(writeToCacheAsynchronously)
)

/**
 * Sets the optimistic updates to write to the cache while a query is pending.
 */
fun <D : Mutation.Data> ApolloRequest.Builder<D>.optimisticUpdates(data: D) = addExecutionContext(
    OptimisticUpdatesContext(data)
)

fun <D : Mutation.Data> ApolloCall<D>.optimisticUpdates(data: D) = addExecutionContext(
    OptimisticUpdatesContext(data)
)

internal val <D : Operation.Data> ApolloRequest<D>.fetchPolicyInterceptor
  get() = executionContext[FetchPolicyContext]?.interceptor ?: CacheFirstInterceptor

internal val <D : Operation.Data> ApolloRequest<D>.refetchPolicyInterceptor
  get() = executionContext[RefetchPolicyContext]?.interceptor ?: CacheOnlyInterceptor

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

internal val <D : Operation.Data> ApolloRequest<D>.watchContext: WatchContext?
  get() = executionContext[WatchContext]

/**
 * @param isCacheHit true if this was a cache hit
 * @param cacheException the exception while reading the cache. Note that it's possible to have [isCacheHit] == false && [cacheException] == null
 * if no cache read was attempted
 */
class CacheInfo private constructor(
    val cacheStartMillis: Long,
    val cacheEndMillis: Long,
    val networkStartMillis: Long,
    val networkEndMillis: Long,
    val isCacheHit: Boolean,
    val cacheMissException: CacheMissException?,
    val networkException: ApolloException?,
) : ExecutionContext.Element {

  @Deprecated("Use CacheInfo.Builder")
  constructor(
      millisStart: Long,
      millisEnd: Long,
      hit: Boolean,
      missedKey: String?,
      missedField: String?,
  ) : this(
      cacheStartMillis = millisStart,
      cacheEndMillis = millisEnd,
      networkStartMillis = 0,
      networkEndMillis = 0,
      isCacheHit = hit,
      cacheMissException = missedKey?.let { CacheMissException(it, missedField) },
      networkException = null
  )

  override val key: ExecutionContext.Key<*>
    get() = Key

  @Deprecated("Use cacheStartMillis instead", ReplaceWith("cacheStartMillis"))
  val millisStart: Long
    get() = cacheStartMillis

  @Deprecated("Use cacheEndMillis instead", ReplaceWith("cacheEndMillis"))
  val millisEnd: Long
    get() = cacheEndMillis

  @Deprecated("Use cacheHit instead", ReplaceWith("cacheHit"))
  val hit: Boolean
    get() = isCacheHit

  @Deprecated("Use cacheMissException?.key instead", ReplaceWith("cacheMissException?.key"))
  val missedKey: String?
    get() = cacheMissException?.key

  @Deprecated("Use cacheMissException?.fieldName instead", ReplaceWith("cacheMissException?.fieldName"))
  val missedField: String?
    get() = cacheMissException?.fieldName


  companion object Key : ExecutionContext.Key<CacheInfo>

  fun newBuilder(): Builder {
    return Builder().cacheStartMillis(cacheStartMillis)
        .cacheEndMillis(cacheEndMillis)
        .networkStartMillis(networkStartMillis)
        .networkEndMillis(networkEndMillis)
        .cacheHit(isCacheHit)
        .networkException(networkException)
  }

  class Builder {
    private var cacheStartMillis: Long = 0
    private var cacheEndMillis: Long = 0
    private var networkStartMillis: Long = 0
    private var networkEndMillis: Long = 0
    private var cacheHit: Boolean = false
    private var cacheMissException: CacheMissException? = null
    private var networkException: ApolloException? = null

    fun cacheStartMillis(cacheStartMillis: Long) = apply {
      this.cacheStartMillis = cacheStartMillis
    }

    fun cacheEndMillis(cacheEndMillis: Long) = apply {
      this.cacheEndMillis = cacheEndMillis
    }

    fun networkStartMillis(networkStartMillis: Long) = apply {
      this.networkStartMillis = networkStartMillis
    }

    fun networkEndMillis(networkEndMillis: Long) = apply {
      this.networkEndMillis = networkEndMillis
    }

    fun cacheHit(cacheHit: Boolean) = apply {
      this.cacheHit = cacheHit
    }

    fun cacheMissException(cacheMissException: CacheMissException?) = apply {
      this.cacheMissException = cacheMissException
    }

    fun networkException(networkException: ApolloException?) = apply {
      this.networkException = networkException
    }


    fun build(): CacheInfo = CacheInfo(
        cacheStartMillis = cacheStartMillis,
        cacheEndMillis = cacheEndMillis,
        networkStartMillis = networkStartMillis,
        networkEndMillis = networkEndMillis,
        isCacheHit = cacheHit,
        cacheMissException = cacheMissException,
        networkException = networkException
    )
  }
}

val <D : Operation.Data> ApolloResponse<D>.isFromCache: Boolean
  get() {
    return cacheInfo?.isCacheHit == true
  }

val <D : Operation.Data> ApolloResponse<D>.cacheInfo
  get() = executionContext[CacheInfo]

internal fun <D : Operation.Data> ApolloResponse<D>.withCacheInfo(cacheInfo: CacheInfo) = newBuilder().addExecutionContext(cacheInfo).build()
internal fun <D : Operation.Data> ApolloResponse.Builder<D>.cacheInfo(cacheInfo: CacheInfo) = addExecutionContext(cacheInfo)

internal class FetchPolicyContext(val interceptor: ApolloInterceptor) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<FetchPolicyContext>
}

internal class RefetchPolicyContext(val interceptor: ApolloInterceptor) : ExecutionContext.Element {
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

internal class WatchContext(val data: Query.Data?) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<WatchContext>
}

internal class FetchFromCacheContext(val value: Boolean) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<FetchFromCacheContext>
}

internal class IsRefetching(val value: Boolean) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<IsRefetching>
}

internal fun <D : Operation.Data> ApolloRequest.Builder<D>.fetchFromCache(fetchFromCache: Boolean) = apply {
  addExecutionContext(FetchFromCacheContext(fetchFromCache))
}

internal fun <D : Operation.Data> ApolloRequest.Builder<D>.isRefetching(isRefetching: Boolean) = apply {
  addExecutionContext(IsRefetching(isRefetching))
}

internal val <D : Operation.Data> ApolloRequest<D>.fetchFromCache
  get() = executionContext[FetchFromCacheContext]?.value ?: false


internal val <D : Operation.Data> ApolloRequest<D>.isRefetching
  get() = executionContext[IsRefetching]?.value ?: false

