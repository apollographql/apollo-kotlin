package com.apollographql.apollo.api.cache.http

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
   * Remove cached http response by key. May throw [IOException]
   *
   * @param cacheKey key of cached response to be removed
   */
  @Throws(IOException::class)
  fun remove(cacheKey: String)

  /**
   * Remove cached http response by key and suppress any exception
   *
   * @param cacheKey key of cached response to be removed
   */
  fun removeQuietly(cacheKey: String)

  /**
   * Read cached http response by key
   *
   * @param cacheKey key of cached response to be read
   * @return cached response
   */
  fun read(cacheKey: String): Response

  /**
   * Read and remove cached http response by key if `expireAfterRead == true`
   *
   * @param cacheKey        key of cached response to be read
   * @param expireAfterRead if `true` cached response will be removed after first read
   * @return cached response
   */
  fun read(cacheKey: String, expireAfterRead: Boolean): Response

  /**
   * Provide http cache interceptor to be injected into [okhttp3.OkHttpClient.interceptors]. Provided interceptor
   * must intercept request and serve cached http response as well as store network response to the http cache store.
   *
   * @return [Interceptor]
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
  }
}
