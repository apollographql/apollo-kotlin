package com.apollographql.apollo.api.cache.http;

import java.io.IOException;

import javax.annotation.Nonnull;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * Http GraphQL http request / response cache.
 */
public interface HttpCache {
  /**
   * Cache key http header
   */
  String CACHE_KEY_HEADER = "X-APOLLO-CACHE-KEY";
  /**
   * Cache fetch strategy http header
   */
  String CACHE_FETCH_STRATEGY_HEADER = "X-APOLLO-CACHE-FETCH-STRATEGY";
  /**
   * Request served Date/time http header
   */
  String CACHE_SERVED_DATE_HEADER = "X-APOLLO-SERVED-DATE";
  /**
   * Prefetch response only flag http header
   */
  String CACHE_PREFETCH_HEADER = "X-APOLLO-PREFETCH";
  /**
   * Cached response expiration timeout http header
   */
  String CACHE_EXPIRE_TIMEOUT_HEADER = "X-APOLLO-EXPIRE-TIMEOUT";
  /**
   * Expire cached response flag http header
   */
  String CACHE_EXPIRE_AFTER_READ_HEADER = "X-APOLLO-EXPIRE-AFTER-READ";

  /**
   * Clear cached http responses
   */
  void clear();

  /**
   * Remove cached http response by key. May throw {@link IOException}
   *
   * @param cacheKey key of cached response to be removed
   */
  void remove(@Nonnull String cacheKey) throws IOException;

  /**
   * Remove cached http response by key and suppress any exception
   *
   * @param cacheKey key of cached response to be removed
   */
  void removeQuietly(@Nonnull String cacheKey);

  /**
   * Read cached http response by key
   *
   * @param cacheKey key of cached response to be read
   * @return cached response
   */
  Response read(@Nonnull String cacheKey);

  /**
   * Read and remove cached http response by key if {@code expireAfterRead == true}
   *
   * @param cacheKey        key of cached response to be read
   * @param expireAfterRead if {@code true} cached response will be removed after first read
   * @return cached response
   */
  Response read(@Nonnull String cacheKey, boolean expireAfterRead);

  /**
   * Provide http cache interceptor to be injected into {@link okhttp3.OkHttpClient#interceptors}. Provided interceptor
   * must intercept request and serve cached http response as well as store network response to the http cache store.
   *
   * @return {@link Interceptor}
   */
  Interceptor interceptor();
}
