@file:JvmName("HttpCache")

package com.apollographql.apollo3.cache.http

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.MutableExecutionOptions
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.network.http.HttpInfo
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

  return addHttpInterceptor(
      CachingHttpInterceptor(
          directory = directory,
          maxSize = maxSize,
      )
  )
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
  val policyStr = when (httpFetchPolicy) {
    HttpFetchPolicy.CacheFirst -> CachingHttpInterceptor.CACHE_FIRST
    HttpFetchPolicy.CacheOnly -> CachingHttpInterceptor.CACHE_ONLY
    HttpFetchPolicy.NetworkFirst -> CachingHttpInterceptor.NETWORK_FIRST
    HttpFetchPolicy.NetworkOnly -> CachingHttpInterceptor.NETWORK_ONLY
  }

  return addHttpHeader(
      CachingHttpInterceptor.CACHE_FETCH_POLICY_HEADER, policyStr
  )
}

/**
 * Configures httpExpireTimeout. Entries will be removed from the cache after this timeout.
 */
fun <T> MutableExecutionOptions<T>.httpExpireTimeout(httpExpireTimeout: Long)  = addHttpHeader(
    CachingHttpInterceptor.CACHE_EXPIRE_TIMEOUT_HEADER, httpExpireTimeout.toString()
)

/**
 * Configures httpExpireAfterRead. Entries will be removed from the cache after read if set to true.
 */
fun <T> MutableExecutionOptions<T>.httpExpireAfterRead(httpExpireAfterRead: Boolean)  = addHttpHeader(
    CachingHttpInterceptor.CACHE_EXPIRE_AFTER_READ_HEADER, httpExpireAfterRead.toString()
)

/**
 * Configures httpDoNotStore. Entries will never be stored if set to true.
 */
fun <T> MutableExecutionOptions<T>.httpDoNotStore(httpDoNotStore: Boolean)  = addHttpHeader(
    CachingHttpInterceptor.CACHE_DO_NOT_STORE, httpDoNotStore.toString()
)
