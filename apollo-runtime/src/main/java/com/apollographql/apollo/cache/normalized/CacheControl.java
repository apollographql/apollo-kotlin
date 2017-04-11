package com.apollographql.apollo.cache.normalized;

/**
 * CacheControl class represents the cache controlling strategies for reading GraphQL responses back from the
 * {@link NormalizedCache}.
 */
public enum CacheControl {

  /**
   * Signals the apollo client to first fetch the data from the normalized cache. If it's not present in the
   * normalized cache or if an exception occurs while trying to fetch it from the normalized cache, then the data is
   * instead fetched from the network.
   */
  CACHE_FIRST,
  /**
   * Signals the apollo client to <b>only</b> fetch the data from the normalized cache. If it's not present in
   * the normalized cache or if an exception occurs while trying to fetch it from the normalized cache, an empty {@link
   * com.apollographql.apollo.api.Response} is sent back with the {@link com.apollographql.apollo.api.Operation} info
   * wrapped inside.
   */
  CACHE_ONLY,
  /**
   * Signals the apollo client to first fetch the data from the network. If network request fails, then the
   * data is fetched from the normalized cache. If the data is not present in the normalized cache, then the
   * exception which led to the network request failure is rethrown.
   */
  NETWORK_FIRST,
  /**
   * Signals the apollo client to <b>only</b> fetch the GraphQL data from the network. If network request fails, an
   * exception is thrown.
   */
  NETWORK_ONLY,
}
