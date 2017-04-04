package com.apollographql.apollo.cache.http;

/**
 * HttpCacheControl class represents the cache controlling strategies for reading http responses from the cache when a
 * network request is made.
 */
public enum HttpCacheControl {

  /**
   * Signals the apollo client to first fetch the data from the http cache. If the data has become stale, then it is
   * fetched from the network.
   */
  CACHE_FIRST("cache-first"),
  /**
   * Signals the apollo client to only fetch the data from the http cache. If the data has become stale, an
   * {@link com.apollographql.apollo.exception.ApolloHttpException} is thrown.
   */
  CACHE_ONLY("cache-only"),
  /**
   * Signals the apollo client to only fetch the data from the network request. If the network request fails, an {@link
   * com.apollographql.apollo.exception.ApolloHttpException} is thrown.
   */
  NETWORK_ONLY("network-only"),
  /**
   * Signals the apollo client to first fetch the data from the network request. If the network request fails, then the
   * data is fetched from the http cache. If the data in the cache has become stale, an {@link
   * com.apollographql.apollo.exception.ApolloHttpException} is thrown.
   */
  NETWORK_FIRST("network-first"),
  /**
   * Signals the apollo client to first fetch the data from the network request. If the network request fails, then the
   * data is fetched from the http cache even if it has become stale.
   */
  NETWORK_BEFORE_STALE("network-before-stale"),
  /**
   * Signals the apollo client to mark the data in the cache stale whenever a read takes place next.
   */
  EXPIRE_AFTER_READ("expire-after-read");

  public final String httpHeader;

  HttpCacheControl(String httpHeader) {
    this.httpHeader = httpHeader;
  }

  public static HttpCacheControl valueOfHttpHeader(String header) {
    for (HttpCacheControl value : values()) {
      if (value.httpHeader.equals(header)) {
        return value;
      }
    }
    return null;
  }
}
