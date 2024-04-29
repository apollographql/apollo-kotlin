@file:JvmName("HttpCache")

package com.apollographql.apollo3.cache.http

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.MutableExecutionOptions
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.api.http.valueOf
import com.apollographql.apollo3.cache.http.internal.HttpCacheApolloInterceptor
import com.apollographql.apollo3.network.http.HttpInfo
import com.apollographql.apollo3.network.http.HttpInterceptor
import com.apollographql.apollo3.network.http.HttpInterceptorChain
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import okio.FileSystem
import java.io.File

enum class HttpFetchPolicy {
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
fun ApolloClient.Builder.httpCache(
    directory: File,
    maxSize: Long,
): ApolloClient.Builder {
  return httpCache(DiskLruHttpCache(FileSystem.SYSTEM, directory, maxSize))
}

@JvmName("configureApolloClientBuilder")
fun ApolloClient.Builder.httpCache(
    apolloHttpCache: ApolloHttpCache,
): ApolloClient.Builder {
  val cachingHttpInterceptor = CachingHttpInterceptor(apolloHttpCache)

  val apolloRequestToCacheKey = mutableMapOf<String, String>()
  return addHttpInterceptor(object : HttpInterceptor {
    override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
      val cacheKey = CachingHttpInterceptor.cacheKey(request)
      val requestUuid = request.headers.valueOf(CachingHttpInterceptor.REQUEST_UUID_HEADER)!!
      synchronized(apolloRequestToCacheKey) {
        apolloRequestToCacheKey[requestUuid] = cacheKey
      }
      return chain.proceed(
          request.newBuilder()
              .headers(request.headers.filterNot { it.name == CachingHttpInterceptor.REQUEST_UUID_HEADER })
              .addHeader(CachingHttpInterceptor.CACHE_KEY_HEADER, cacheKey)
              .build()
      )
    }
  })
      .addHttpInterceptor(cachingHttpInterceptor)
      .apply {
        // Remove any existing HttpCacheApolloInterceptor
        interceptors.firstOrNull { it is HttpCacheApolloInterceptor }?.let {
          removeInterceptor(it)
        }
      }
      .addInterceptor(HttpCacheApolloInterceptor(apolloRequestToCacheKey, cachingHttpInterceptor))
}


val <D : Operation.Data> ApolloResponse<D>.isFromHttpCache
  get() = executionContext[HttpInfo]?.headers?.any {
    // This will return true whatever the value in the header. We might want to fine tune this
    it.name == CachingHttpInterceptor.FROM_CACHE
  } ?: false

/**
 * Configures the [HttpFetchPolicy]
 */
fun <T> MutableExecutionOptions<T>.httpFetchPolicy(httpFetchPolicy: HttpFetchPolicy): T {
  return addExecutionContext(HttpFetchPolicyContext(httpFetchPolicy))
}

/**
 * Configures httpExpireTimeout. Entries will be removed from the cache after this timeout.
 */
fun <T> MutableExecutionOptions<T>.httpExpireTimeout(httpExpireTimeout: Long) = addHttpHeader(
    CachingHttpInterceptor.CACHE_EXPIRE_TIMEOUT_HEADER, httpExpireTimeout.toString()
)

/**
 * Configures httpExpireAfterRead. Entries will be removed from the cache after read if set to true.
 */
fun <T> MutableExecutionOptions<T>.httpExpireAfterRead(httpExpireAfterRead: Boolean) = addHttpHeader(
    CachingHttpInterceptor.CACHE_EXPIRE_AFTER_READ_HEADER, httpExpireAfterRead.toString()
)

/**
 * Configures httpDoNotStore. Entries will never be stored if set to true.
 */
fun <T> MutableExecutionOptions<T>.httpDoNotStore(httpDoNotStore: Boolean) = addHttpHeader(
    CachingHttpInterceptor.CACHE_DO_NOT_STORE, httpDoNotStore.toString()
)

val ApolloClient.httpCache: ApolloHttpCache
  get() {
    val httpNetworkTransport = networkTransport as? HttpNetworkTransport
        ?: error("cannot get the HttpCache, networkTransport is not a HttpNetworkTransport")
    val cachingHttpInterceptor = httpNetworkTransport.interceptors.firstOrNull { it is CachingHttpInterceptor }
        ?: error("no http cache configured")

    return (cachingHttpInterceptor as CachingHttpInterceptor).cache
  }

