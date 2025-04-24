@file:JvmName("NormalizedCache")

package com.apollographql.apollo.cache.normalized

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.CacheDumpProviderContext
import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloDeprecatedSince.Version.v4_0_0
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.MutableExecutionOptions
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.http.get
import com.apollographql.apollo.cache.normalized.api.ApolloCacheHeaders
import com.apollographql.apollo.cache.normalized.api.CacheHeaders
import com.apollographql.apollo.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo.cache.normalized.api.CacheResolver
import com.apollographql.apollo.cache.normalized.api.FieldPolicyCacheResolver
import com.apollographql.apollo.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo.cache.normalized.internal.ApolloCacheInterceptor
import com.apollographql.apollo.cache.normalized.internal.WatcherInterceptor
import com.apollographql.apollo.cache.normalized.internal.WatcherSentinel
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.CacheMissException
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.apollo.interceptor.AutoPersistedQueryInterceptor
import com.apollographql.apollo.mpp.currentTimeMillis
import com.apollographql.apollo.network.http.HttpInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

enum class FetchPolicy {
  /**
   * Try the cache, if that failed, try the network.
   *
   * This [FetchPolicy] emits one or more [ApolloResponse]s.
   * Cache misses and network errors have [ApolloResponse.exception] set to a non-null [ApolloException]
   *
   * This is the default behaviour.
   */
  CacheFirst,

  /**
   * Only try the cache.
   *
   * This [FetchPolicy] emits one [ApolloResponse].
   * Cache misses have [ApolloResponse.exception] set to a non-null [ApolloException]
   */
  CacheOnly,

  /**
   * Try the network, if that failed, try the cache.
   *
   * This [FetchPolicy] emits one or more [ApolloResponse]s.
   * Cache misses and network errors have [ApolloResponse.exception] set to a non-null [ApolloException]
   */
  NetworkFirst,

  /**
   * Only try the network.
   *
   * This [FetchPolicy] emits one or more [ApolloResponse]s. Several [ApolloResponse]s
   * may be emitted if your [NetworkTransport] supports it, for example with `@defer`.
   * Network errors have [ApolloResponse.exception] set to a non-null [ApolloException]
   */
  NetworkOnly,

  /**
   * Try the cache, then also try the network.
   *
   * This [FetchPolicy] emits two or more [ApolloResponse]s.
   * Cache misses and network errors have [ApolloResponse.exception] set to a non-null [ApolloException]
   */
  CacheAndNetwork,
}

/**
 * Configures an [ApolloClient] with a normalized cache.
 *
 * @param normalizedCacheFactory a factory that creates a [com.apollographql.apollo.cache.normalized.api.NormalizedCache].
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
  check(interceptors.none { it is AutoPersistedQueryInterceptor }) {
    "Apollo: the normalized cache must be configured before the auto persisted queries"
  }
  // Removing existing interceptors added for configuring an [ApolloStore].
  // If a builder is reused from an existing client using `newBuilder()` and we try to configure a new `store()` on it, we first need to
  // remove the old interceptors.
  val storeInterceptors = interceptors.filterIsInstance<ApolloStoreInterceptor>()
  storeInterceptors.forEach {
    removeInterceptor(it)
  }
  return addInterceptor(WatcherInterceptor(store))
      .addInterceptor(FetchPolicyRouterInterceptor)
      .addInterceptor(ApolloCacheInterceptor(store))
      .writeToCacheAsynchronously(writeToCacheAsynchronously)
      .addExecutionContext(CacheDumpProviderContext(store.cacheDumpProvider()))
}

@Deprecated(level = DeprecationLevel.ERROR, message = "Exceptions no longer throw", replaceWith = ReplaceWith("watch()"))
@ApolloDeprecatedSince(v4_0_0)
@Suppress("UNUSED_PARAMETER")
fun <D : Query.Data> ApolloCall<D>.watch(
    fetchThrows: Boolean,
    refetchThrows: Boolean,
): Flow<ApolloResponse<D>> = throw UnsupportedOperationException("watch(fetchThrows: Boolean, refetchThrows: Boolean) is no longer supported, use watch() instead")

@Deprecated(level = DeprecationLevel.ERROR, message = "Exceptions no longer throw", replaceWith = ReplaceWith("watch()"))
@ApolloDeprecatedSince(v4_0_0)
@Suppress("UNUSED_PARAMETER")
fun <D : Query.Data> ApolloCall<D>.watch(
    fetchThrows: Boolean,
): Flow<ApolloResponse<D>> = throw UnsupportedOperationException("watch(fetchThrows: Boolean, refetchThrows: Boolean) is no longer supported, use watch() instead")

/**
 * Gets initial response(s) then observes the cache for any changes.
 *
 * There is a guarantee that the cache is subscribed before the initial response(s) finish emitting. Any update to the cache done after the initial response(s) are received will be received.
 *
 * [fetchPolicy] controls how the result is first queried, while [refetchPolicy] will control the subsequent fetches.
 *
 * @see fetchPolicy
 * @see refetchPolicy
 */
fun <D : Query.Data> ApolloCall<D>.watch(): Flow<ApolloResponse<D>> {
  return flow {
    var lastResponse: ApolloResponse<D>? = null
    var response: ApolloResponse<D>? = null

    toFlow()
        .collect {
          response = it

          if (it.isLast) {
            if (lastResponse != null) {
              /**
               * If we ever come here it means some interceptors built a new Flow and forgot to reset the isLast flag
               * Better safe than sorry: emit them when we realize that. This will introduce a delay in the response.
               */
              println("ApolloGraphQL: extra response received after the last one")
              emit(lastResponse!!)
            }
            /**
             * Remember the last response so that we can send it after we subscribe to the store
             *
             * This allows callers to use the last element as a synchronisation point to modify the store and still have the watcher
             * receive subsequent updates
             *
             * See https://github.com/apollographql/apollo-kotlin/pull/3853
             */
            lastResponse = it
          } else {
            emit(it)
          }
        }


    copy().fetchPolicyInterceptor(refetchPolicyInterceptor)
        .watchInternal(response?.data)
        .collect {
          if (it.exception === WatcherSentinel) {
            if (lastResponse != null) {
              emit(lastResponse!!)
              lastResponse = null
            }
          } else {
            emit(it)
          }
        }
  }
}

/**
 * Observes the cache for the given data. Unlike [watch], no initial request is executed on the network.
 * The fetch policy set by [fetchPolicy] will be used.
 */
fun <D : Query.Data> ApolloCall<D>.watch(data: D?): Flow<ApolloResponse<D>> {
  return watchInternal(data).filter { it.exception !== WatcherSentinel }
}

/**
 * Observes the cache for the given data. Unlike [watch], no initial request is executed on the network.
 * The fetch policy set by [fetchPolicy] will be used.
 */
internal fun <D : Query.Data> ApolloCall<D>.watchInternal(data: D?): Flow<ApolloResponse<D>> {
  return copy().addExecutionContext(WatchContext(data)).toFlow()
}

val ApolloClient.apolloStore: ApolloStore
  get() {
    return interceptors.firstOrNull { it is ApolloCacheInterceptor }?.let {
      (it as ApolloCacheInterceptor).store
    } ?: error("no cache configured")
  }

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
  FetchPolicy.CacheAndNetwork -> CacheAndNetworkInterceptor
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
 * @param memoryOnly Whether to store and read from a memory cache only.
 *
 * Default: false
 */
fun <T> MutableExecutionOptions<T>.memoryCacheOnly(memoryOnly: Boolean) = addExecutionContext(
    MemoryCacheOnlyContext(memoryOnly)
)

@Deprecated("Emitting cache misses is now the default behavior, this method is a no-op", replaceWith = ReplaceWith(""))
@ApolloDeprecatedSince(v4_0_0)
@Suppress("UNUSED_PARAMETER")
fun <T> MutableExecutionOptions<T>.emitCacheMisses(emitCacheMisses: Boolean) = this

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
 * @param storeReceiveDate Whether to store the receive date in the cache.
 *
 * Default: false
 */
@ApolloExperimental
fun <T> MutableExecutionOptions<T>.storeReceiveDate(storeReceiveDate: Boolean) = addExecutionContext(
    StoreReceiveDateContext(storeReceiveDate)
)

/**
 * @param storeExpirationDate Whether to store the expiration date in the cache.
 *
 * The expiration date is computed from the response HTTP headers
 *
 * Default: false
 */
@ApolloExperimental
fun <T> MutableExecutionOptions<T>.storeExpirationDate(storeExpirationDate: Boolean): T {
  addExecutionContext(StoreExpirationDateContext(storeExpirationDate))
  if (this is ApolloClient.Builder) {
    check(interceptors.none { it is StoreExpirationInterceptor }) {
      "Apollo: storeExpirationDate() can only be called once on ApolloClient.Builder()"
    }
    addInterceptor(StoreExpirationInterceptor())
  }
  @Suppress("UNCHECKED_CAST")
  return this as T
}

private class StoreExpirationInterceptor: ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return chain.proceed(request).map {
      val store = request.executionContext[StoreExpirationDateContext]?.value
      if (store != true) {
        return@map it
      }
      val headers = it.executionContext[HttpInfo]?.headers.orEmpty()

      val cacheControl = headers.get("cache-control")?.lowercase() ?: return@map it

      val c = cacheControl.split(",").map { it.trim() }
      val maxAge = c.mapNotNull {
        if (it.startsWith("max-age=")) {
          it.substring(8).toIntOrNull()
        } else {
          null
        }
      }.firstOrNull() ?: return@map it

      val age = headers.get("age")?.toIntOrNull()
      val expires = if (age != null) {
        currentTimeMillis() / 1000 + maxAge - age
      } else {
        currentTimeMillis() / 1000 + maxAge
      }

      return@map it.newBuilder()
          .cacheHeaders(
              it.cacheHeaders.newBuilder()
                  .addHeader(ApolloCacheHeaders.DATE, expires.toString())
                  .build()
          )
          .build()
    }
  }
}

/**
 * @param cacheHeaders additional cache headers to be passed to your [com.apollographql.apollo.cache.normalized.api.NormalizedCache]
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

internal val <D : Operation.Data> ApolloCall<D>.fetchPolicyInterceptor
  get() = executionContext[FetchPolicyContext]?.interceptor ?: CacheFirstInterceptor

private val <T> MutableExecutionOptions<T>.refetchPolicyInterceptor
  get() = executionContext[RefetchPolicyContext]?.interceptor ?: CacheOnlyInterceptor

internal val <D : Operation.Data> ApolloRequest<D>.doNotStore
  get() = executionContext[DoNotStoreContext]?.value ?: false

internal val <D : Operation.Data> ApolloRequest<D>.memoryCacheOnly
  get() = executionContext[MemoryCacheOnlyContext]?.value ?: false

internal val <D : Operation.Data> ApolloRequest<D>.storePartialResponses
  get() = executionContext[StorePartialResponsesContext]?.value ?: false

internal val <D : Operation.Data> ApolloRequest<D>.storeReceiveDate
  get() = executionContext[StoreReceiveDateContext]?.value ?: false

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
 * @param cacheMissException the exception while reading the cache. Note that it's possible to have [isCacheHit] == false && [cacheMissException] == null
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

/**
 * True if this response comes from the cache, false if it comes from the network.
 *
 * Note that this can be true regardless of whether the data was found in the cache.
 * To know whether the **data** is from the cache, use `cacheInfo?.isCacheHit == true`.
 */
val <D : Operation.Data> ApolloResponse<D>.isFromCache: Boolean
  get() {
    return cacheInfo?.isCacheHit == true || exception is CacheMissException
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

internal class MemoryCacheOnlyContext(val value: Boolean) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<MemoryCacheOnlyContext>
}

internal class StorePartialResponsesContext(val value: Boolean) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<StorePartialResponsesContext>
}

internal class StoreReceiveDateContext(val value: Boolean) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<StoreReceiveDateContext>
}

internal class StoreExpirationDateContext(val value: Boolean) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<StoreExpirationDateContext>
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

internal class WatchContext(
    val data: Query.Data?,
) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<WatchContext>
}

internal class FetchFromCacheContext(val value: Boolean) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<FetchFromCacheContext>
}

internal fun <D : Operation.Data> ApolloRequest.Builder<D>.fetchFromCache(fetchFromCache: Boolean) = apply {
  addExecutionContext(FetchFromCacheContext(fetchFromCache))
}

internal val <D : Operation.Data> ApolloRequest<D>.fetchFromCache
  get() = executionContext[FetchFromCacheContext]?.value ?: false

fun <D : Operation.Data> ApolloResponse.Builder<D>.cacheHeaders(cacheHeaders: CacheHeaders) =
    addExecutionContext(CacheHeadersContext(cacheHeaders))

val <D : Operation.Data> ApolloResponse<D>.cacheHeaders
  get() = executionContext[CacheHeadersContext]?.value ?: CacheHeaders.NONE

/**
 * Gets the result from the cache first and always fetch from the network. Use this to get an early
 * cached result while also updating the network values.
 *
 * Any [FetchPolicy] previously set will be ignored
 */
@Deprecated("Use fetchPolicy(FetchPolicy.CacheAndNetwork) instead", ReplaceWith("fetchPolicy(FetchPolicy.CacheAndNetwork).toFlow()"), level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_7_5)
fun <D : Query.Data> ApolloCall<D>.executeCacheAndNetwork(): Flow<ApolloResponse<D>> = TODO()
