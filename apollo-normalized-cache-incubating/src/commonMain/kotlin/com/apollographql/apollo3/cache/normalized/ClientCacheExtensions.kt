@file:JvmName("NormalizedCache")

package com.apollographql.apollo3.cache.normalized

import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_0_0
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.MutableExecutionOptions
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.http.get
import com.apollographql.apollo3.cache.normalized.api.ApolloCacheHeaders
import com.apollographql.apollo3.cache.normalized.api.ApolloResolver
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.CacheResolver
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyCacheResolver
import com.apollographql.apollo3.cache.normalized.api.MetadataGenerator
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.api.RecordMerger
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.internal.ApolloCacheInterceptor
import com.apollographql.apollo3.cache.normalized.internal.WatcherInterceptor
import com.apollographql.apollo3.exception.ApolloCompositeException
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.CacheMissException
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.mpp.currentTimeMillis
import com.apollographql.apollo3.network.http.HttpInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

enum class FetchPolicy {
  /**
   * Try the cache, if that failed, try the network.
   *
   * An [ApolloCompositeException] is thrown if the data is not in the cache and the network call failed.
   * If coming from the cache 1 value is emitted, otherwise 1 or multiple values can be emitted from the network.
   *
   * This is the default behaviour.
   */
  CacheFirst,

  /**
   * Only try the cache.
   *
   * A [CacheMissException] is thrown if the data is not in the cache, otherwise 1 value is emitted.
   */
  CacheOnly,

  /**
   * Try the network, if that failed, try the cache.
   *
   * An [ApolloCompositeException] is thrown if the network call failed and the data is not in the cache.
   * If coming from the network 1 or multiple values can be emitted, otherwise 1 value is emitted from the cache.
   */
  NetworkFirst,

  /**
   * Only try the network.
   *
   * An [ApolloException] is thrown if the network call failed, otherwise 1 or multiple values can be emitted.
   */
  NetworkOnly,

  /**
   * Try the cache, then also try the network.
   *
   * If the data is in the cache, it is emitted, if not, no exception is thrown at that point. Then the network call is made, and if
   * successful the value(s) are emitted, otherwise either an [ApolloCompositeException] (both cache miss and network failed) or an
   * [ApolloException] (only network failed) is thrown.
   */
  CacheAndNetwork,
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

@ApolloExperimental
@JvmOverloads
@JvmName("configureApolloClientBuilder2")
fun ApolloClient.Builder.normalizedCache(
    normalizedCacheFactory: NormalizedCacheFactory,
    cacheKeyGenerator: CacheKeyGenerator,
    metadataGenerator: MetadataGenerator,
    apolloResolver: ApolloResolver,
    recordMerger: RecordMerger,
    writeToCacheAsynchronously: Boolean = false,
): ApolloClient.Builder {
  return store(
      ApolloStore(
          normalizedCacheFactory = normalizedCacheFactory,
          cacheKeyGenerator = cacheKeyGenerator,
          metadataGenerator = metadataGenerator,
          apolloResolver = apolloResolver,
          recordMerger = recordMerger
      ), writeToCacheAsynchronously)
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

/**
 * Gets the result from the network, then observes the cache for any changes.
 * [fetchPolicy] will control how the result is first queried, while [refetchPolicy] will control the subsequent fetches.
 * Network and cache exceptions are ignored by default, this can be changed by setting [fetchThrows] for the first fetch and [refetchThrows]
 * for subsequent fetches (non Apollo exceptions like `OutOfMemoryError` are always propagated).
 *
 * @param fetchThrows whether to throw if an [ApolloException] happens during the initial fetch. Default: false
 * @param refetchThrows whether to throw if an [ApolloException] happens during a refetch. Default: false
 */
@JvmOverloads
fun <D : Query.Data> ApolloCall<D>.watch(
    fetchThrows: Boolean = false,
    refetchThrows: Boolean = false,
): Flow<ApolloResponse<D>> {
  return flow {
    var lastResponse: ApolloResponse<D>? = null
    var response: ApolloResponse<D>? = null

    toFlow()
        .catch {
          if (it !is ApolloException || fetchThrows) throw it
        }.collect {
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
        .watch(response?.data) { _, _ ->
          // If the exception is ignored (refetchThrows is false), we should continue watching - so retry
          !refetchThrows
        }.onStart {
          if (lastResponse != null) {
            emit(lastResponse!!)
          }
        }.collect {
          emit(it)
        }
  }
}

/**
 * Observes the cache for the given data. Unlike [watch], no initial request is executed on the network.
 * Network and cache exceptions are ignored by default, this can be controlled with the [retryWhen] lambda.
 * The fetch policy set by [fetchPolicy] will be used.
 */
fun <D : Query.Data> ApolloCall<D>.watch(
    data: D?,
    retryWhen: suspend (cause: Throwable, attempt: Long) -> Boolean = { _, _ -> true },
): Flow<ApolloResponse<D>> {
  return copy().addExecutionContext(WatchContext(data, retryWhen)).toFlow()
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
          first = cacheException,
          second = networkException
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
 * @param emitCacheMisses Whether to emit cache misses instead of throwing.
 * The returned response will have `response.data == null`
 * You can read `response.cacheInfo` to get more information about the cache miss
 *
 * Default: false
 */
fun <T> MutableExecutionOptions<T>.emitCacheMisses(emitCacheMisses: Boolean) = addExecutionContext(
    EmitCacheMissesContext(emitCacheMisses)
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

private class StoreExpirationInterceptor : ApolloInterceptor {
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

internal val <D : Operation.Data> ApolloCall<D>.fetchPolicyInterceptor
  get() = executionContext[FetchPolicyContext]?.interceptor ?: CacheFirstInterceptor

private val <T> MutableExecutionOptions<T>.refetchPolicyInterceptor
  get() = executionContext[RefetchPolicyContext]?.interceptor ?: CacheOnlyInterceptor

internal val <D : Operation.Data> ApolloRequest<D>.doNotStore
  get() = executionContext[DoNotStoreContext]?.value ?: false

internal val <D : Operation.Data> ApolloRequest<D>.storePartialResponses
  get() = executionContext[StorePartialResponsesContext]?.value ?: false

internal val <D : Operation.Data> ApolloRequest<D>.storeReceiveDate
  get() = executionContext[StoreReceiveDateContext]?.value ?: false

internal val <D : Operation.Data> ApolloRequest<D>.emitCacheMisses
  get() = executionContext[EmitCacheMissesContext]?.value ?: false

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

internal class EmitCacheMissesContext(val value: Boolean) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<EmitCacheMissesContext>
}

internal class OptimisticUpdatesContext<D : Mutation.Data>(val value: D) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<OptimisticUpdatesContext<*>>
}

internal class WatchContext(
    val data: Query.Data?,
    val retryWhen: suspend (cause: Throwable, attempt: Long) -> Boolean,
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
