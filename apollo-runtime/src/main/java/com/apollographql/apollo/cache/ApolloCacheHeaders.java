package com.apollographql.apollo.cache;

import com.apollographql.apollo.cache.normalized.NormalizedCache;

/**
 * A collection of cache headers that Apollo's implementations of {@link NormalizedCache} respect.
 */
public final class ApolloCacheHeaders {

  /**
   * Records from this request should not be stored in the {@link NormalizedCache}.
   */
  public static final String DO_NOT_STORE = "do-not-store";

  /**
   * Records from this request should be evicted after being read.
   */
  public static final String EVICT_AFTER_READ = "evict-after-read";
}
