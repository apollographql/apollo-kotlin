package com.apollographql.apollo3.api.cache.http

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Http GraphQL http request / response cache.
 */
interface HttpCache {
  /**
   * Clear cached http responses
   */
  fun clear()

  /**
   * Remove cached http response by [cacheKey]. May throw [IOException]
   */
  @Throws(IOException::class)
  fun remove(cacheKey: String)

  /**
   * Remove cached http response by [cacheKey] and suppress any exception
   */
  fun removeQuietly(cacheKey: String)

  /**
   * Read and return cached http response by [cacheKey]
   */
  fun read(cacheKey: String): Response?

  /**
   * Read and return cached http response by [cacheKey]
   * if [expireAfterRead] is `true`, then cached response will be removed after first read
   */
  fun read(cacheKey: String, expireAfterRead: Boolean): Response?

  /**
   * Provide http cache [Interceptor] to be injected into [okhttp3.OkHttpClient.interceptors]. Provided interceptor
   * must intercept request and serve cached http response as well as store network response to the http cache store.
   */
  fun interceptor(): Interceptor

  companion object {
    /**
     * Cache key http header
     */
    const val CACHE_KEY_HEADER = "X-APOLLO-CACHE-KEY"

    /**
     * Cache fetch strategy http header
     */
    const val CACHE_FETCH_STRATEGY_HEADER = "X-APOLLO-CACHE-FETCH-STRATEGY"

    /**
     * Request served Date/time http header
     */
    const val CACHE_SERVED_DATE_HEADER = "X-APOLLO-SERVED-DATE"

    /**
     * Prefetch response only flag http header
     */
    const val CACHE_PREFETCH_HEADER = "X-APOLLO-PREFETCH"

    /**
     * Cached response expiration timeout http header
     */
    const val CACHE_EXPIRE_TIMEOUT_HEADER = "X-APOLLO-EXPIRE-TIMEOUT"

    /**
     * Expire cached response flag http header
     */
    const val CACHE_EXPIRE_AFTER_READ_HEADER = "X-APOLLO-EXPIRE-AFTER-READ"

    /**
     * Do not store the http response
     */
    const val CACHE_DO_NOT_STORE = "X-APOLLO-CACHE-DO-NOT-STORE"

    /**
     * Signals that HTTP response comes from the local cache
     */
    const val FROM_CACHE = "X-APOLLO-FROM-CACHE"
  }
}
