package com.apollographql.apollo3.cache.normalized.api

/**
 * A collection of cache headers that Apollo's implementations of [NormalizedCache] respect.
 */
object ApolloCacheHeaders {
  /**
   * Records from this request should not be stored in the [NormalizedCache].
   */
  const val DO_NOT_STORE = "do-not-store"

  /**
   * Records from this request should be evicted after being read.
   */
  const val EVICT_AFTER_READ = "evict-after-read"
}
