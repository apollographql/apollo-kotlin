package com.apollographql.android.cache.http;

/**
 * HttpCacheControl class represents the cache controlling strategies for reading http responses from the cache when a
 * network request is made.
 */
public enum HttpCacheControl {

  /**
   * Signals the apollo client to first fetch the data from the cache. If that fails, then
   * the data is fetched from the network.
   */
  CACHE_FIRST("cache-first"),
  /**
   * Signals the apollo client to only fetch data from the cache. An exception is thrown if the cache read
   * fails.
   */
  CACHE_ONLY("cache-only"),
  /**
   * Signals the apollo client to only fetch data from a network request. An exception is thrown if the network request
   * fails.
   */
  NETWORK_ONLY("network-only"),
  /**
   * Signals the apollo client to first fetch the data from the network request. If the network request fails, then
   * the data is fetched from the cache.
   */
  NETWORK_FIRST("network-first"),
  /**
   * Ensures that the stale data is not deleted from the cache in case a network request results in an error.
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

  static HttpCacheControl valueOfHttpHeader(String header) {
    for (HttpCacheControl value : values()) {
      if (value.httpHeader.equals(header)) {
        return value;
      }
    }
    return null;
  }
}
