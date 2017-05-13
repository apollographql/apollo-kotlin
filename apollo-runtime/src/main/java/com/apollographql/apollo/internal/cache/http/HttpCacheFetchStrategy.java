package com.apollographql.apollo.internal.cache.http;

/**
 * Represents different fetch strategies for http request / response cache
 */
public enum HttpCacheFetchStrategy {
  /**
   * Signals the apollo client to fetch the GraphQL query response from the http cache <b>only</b>.
   */
  CACHE_ONLY,
  /**
   * Signals the apollo client to fetch the GraphQL query response from the network <b>only</b>.
   */
  NETWORK_ONLY,
  /**
   * Signals the apollo client to first fetch the GraphQL query response from the http cache. If it's not present in the
   * cache response is fetched from the network.
   */
  CACHE_FIRST,
  /**
   * Signals the apollo client to first fetch the GraphQL query response from the network. If it fails then fetch the
   * response from the http cache.
   */
  NETWORK_FIRST
}
