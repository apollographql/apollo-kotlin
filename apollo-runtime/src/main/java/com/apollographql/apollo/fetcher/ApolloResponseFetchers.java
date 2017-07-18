package com.apollographql.apollo.fetcher;

import com.apollographql.apollo.internal.fetcher.CacheFirstFetcher;
import com.apollographql.apollo.internal.fetcher.CacheOnlyFetcher;
import com.apollographql.apollo.internal.fetcher.CacheAndNetworkFetcher;
import com.apollographql.apollo.internal.fetcher.NetworkFirstFetcher;
import com.apollographql.apollo.internal.fetcher.NetworkOnlyFetcher;

public final class ApolloResponseFetchers {

  /**
   * Signals the apollo client to <b>only</b> fetch the data from the normalized cache. If it's not present in
   * the normalized cache or if an exception occurs while trying to fetch it from the normalized cache, an empty {@link
   * com.apollographql.apollo.api.Response} is sent back with the {@link com.apollographql.apollo.api.Operation} info
   * wrapped inside.
   */
  public static final ResponseFetcher CACHE_ONLY = new CacheOnlyFetcher();

  /**
   * Signals the apollo client to <b>only</b> fetch the GraphQL data from the network. If network request fails, an
   * exception is thrown.
   */
  public static final ResponseFetcher NETWORK_ONLY = new NetworkOnlyFetcher();

  /**
   * Signals the apollo client to first fetch the data from the normalized cache. If it's not present in the
   * normalized cache or if an exception occurs while trying to fetch it from the normalized cache, then the data is
   * instead fetched from the network.
   */
  public static final ResponseFetcher CACHE_FIRST = new CacheFirstFetcher();

  /**
   * Signals the apollo client to first fetch the data from the network. If network request fails, then the
   * data is fetched from the normalized cache. If the data is not present in the normalized cache, then the
   * exception which led to the network request failure is rethrown.
   */
  public static final ResponseFetcher NETWORK_FIRST = new NetworkFirstFetcher();

  /**
   * Signal the apollo client to fetch the data from both the network and the cache. If cached data is not
   * present, only network data will be returned. If cached data is available, but network experiences an error,
   * cached data is returned. If cache data is not available, and network data is not available, the error
   * of the network request will be propagated. If both network and cache are available, both will be returned.
   * Cache data is guaranteed to be returned first.
   */
  public static final ResponseFetcher CACHE_AND_NETWORK = new CacheAndNetworkFetcher();
}
