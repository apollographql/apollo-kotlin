package com.apollographql.apollo.cache;

import com.apollographql.apollo.cache.normalized.*;

/**
 * A collection of cache headers that Apollo's implementations of {@link NormalizedCache} respect.
 */
public interface ApolloCacheHeaders {

  /**
   * Records from this request should not be stored in the {@link NormalizedCache}.
   * This does not specify that a request should not be read from a cache. To control where
   * a response is read use {@link com.apollographql.apollo.cache.normalized.CacheControl}.
   */
  String NO_CACHE = "no-cache";

  /**
   * Records from this request should be evicted after being read.
   */
  String EVICT_AFTER_READ = "evict-after-read";
}
