@file:JvmName("HttpCache")
@file:Suppress("DEPRECATION")
package com.apollographql.apollo.cache.http

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.MutableExecutionOptions
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.cache.http.internal.CacheHeadersHttpInterceptor
import com.apollographql.apollo.cache.http.internal.HttpCacheApolloInterceptor
import com.apollographql.apollo.network.http.HttpInfo
import com.apollographql.apollo.network.http.HttpNetworkTransport
import okio.FileSystem
import java.io.File

@Deprecated("Use `OkHttpEngine(cachePostResponses = true)` instead. See https://go.apollo.dev/ak-http-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
enum class HttpFetchPolicy {
  /**
   * Try cache first, then network
   *
   * This is the default behavior
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

@Deprecated("Use `OkHttpEngine(cachePostResponses = true)` instead. See https://go.apollo.dev/ak-http-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
internal class HttpFetchPolicyContext(val httpFetchPolicy: HttpFetchPolicy) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<HttpFetchPolicyContext>
}

/**
 * Configures a persistent LRU HTTP cache for the ApolloClient.
 *
 * @param directory: the directory where the cache will be persisted
 * @param maxSize: the maxSize in bytes that the cache acn occupy
 *
 * See also [ApolloClient.Builder.httpEngine] and [ApolloClient.Builder.networkTransport]
 */
@JvmName("configureApolloClientBuilder")
@Deprecated("Use `OkHttpEngine(cachePostResponses = true)` instead. See https://go.apollo.dev/ak-http-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
fun ApolloClient.Builder.httpCache(
    directory: File,
    maxSize: Long,
): ApolloClient.Builder {
  return httpCache(DiskLruHttpCache(FileSystem.SYSTEM, directory, maxSize))
}

@JvmName("configureApolloClientBuilder")
@Deprecated("Use `OkHttpEngine(cachePostResponses = true)` instead. See https://go.apollo.dev/ak-http-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
fun ApolloClient.Builder.httpCache(
    apolloHttpCache: ApolloHttpCache,
): ApolloClient.Builder {
  val cachingHttpInterceptor = CachingHttpInterceptor(apolloHttpCache)
  val apolloRequestToCacheKey = mutableMapOf<String, String>()
  return apply {
    httpInterceptors.firstOrNull { it is CacheHeadersHttpInterceptor }?.let {
      removeHttpInterceptor(it)
    }
    httpInterceptors.firstOrNull { it is CachingHttpInterceptor }?.let {
      removeHttpInterceptor(it)
    }
  }
      .addHttpInterceptor(CacheHeadersHttpInterceptor(apolloRequestToCacheKey))
      .addHttpInterceptor(cachingHttpInterceptor)
      .apply {
        interceptors.firstOrNull { it is HttpCacheApolloInterceptor }?.let {
          removeInterceptor(it)
        }
      }
      .addInterceptor(HttpCacheApolloInterceptor(apolloRequestToCacheKey, cachingHttpInterceptor))
}


@Deprecated("Use `OkHttpEngine(cachePostResponses = true)` instead. See https://go.apollo.dev/ak-http-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
val <D : Operation.Data> ApolloResponse<D>.isFromHttpCache
  get() = executionContext[HttpInfo]?.headers?.any {
    // This will return true whatever the value in the header. We might want to fine tune this
    it.name == CachingHttpInterceptor.FROM_CACHE
  } ?: false

/**
 * Configures the [HttpFetchPolicy]
 */
@Deprecated("Use `OkHttpEngine(cachePostResponses = true)` instead. See https://go.apollo.dev/ak-http-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
fun <T> MutableExecutionOptions<T>.httpFetchPolicy(httpFetchPolicy: HttpFetchPolicy): T {
  return addExecutionContext(HttpFetchPolicyContext(httpFetchPolicy))
}

/**
 * Configures httpExpireTimeout. Entries will be removed from the cache after this timeout.
 */
@Deprecated("Use `OkHttpEngine(cachePostResponses = true)` instead. See https://go.apollo.dev/ak-http-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
fun <T> MutableExecutionOptions<T>.httpExpireTimeout(httpExpireTimeout: Long) = addHttpHeader(
    CachingHttpInterceptor.CACHE_EXPIRE_TIMEOUT_HEADER, httpExpireTimeout.toString()
)

/**
 * Configures httpExpireAfterRead. Entries will be removed from the cache after read if set to true.
 */
@Deprecated("Use `OkHttpEngine(cachePostResponses = true)` instead. See https://go.apollo.dev/ak-http-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
fun <T> MutableExecutionOptions<T>.httpExpireAfterRead(httpExpireAfterRead: Boolean) = addHttpHeader(
    CachingHttpInterceptor.CACHE_EXPIRE_AFTER_READ_HEADER, httpExpireAfterRead.toString()
)

/**
 * Configures httpDoNotStore. Entries will never be stored if set to true.
 */
@Deprecated("Use `OkHttpEngine(cachePostResponses = true)` instead. See https://go.apollo.dev/ak-http-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
fun <T> MutableExecutionOptions<T>.httpDoNotStore(httpDoNotStore: Boolean) = addHttpHeader(
    CachingHttpInterceptor.CACHE_DO_NOT_STORE, httpDoNotStore.toString()
)

@Deprecated("Use `OkHttpEngine(cachePostResponses = true)` instead. See https://go.apollo.dev/ak-http-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
val ApolloClient.httpCache: ApolloHttpCache
  get() {
    val httpNetworkTransport = networkTransport as? HttpNetworkTransport
        ?: error("cannot get the HttpCache, networkTransport is not a HttpNetworkTransport")
    val cachingHttpInterceptor = httpNetworkTransport.interceptors.firstOrNull { it is CachingHttpInterceptor }
        ?: error("no http cache configured")

    return (cachingHttpInterceptor as CachingHttpInterceptor).cache
  }

