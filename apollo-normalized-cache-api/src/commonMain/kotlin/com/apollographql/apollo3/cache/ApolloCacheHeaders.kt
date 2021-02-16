package com.apollographql.apollo3.cache

import com.apollographql.apollo3.cache.normalized.NormalizedCache

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

  /**
   * Records from this request should be stored even if the response contains errors, and the records might not be complete.
   */
  const val STORE_PARTIAL_RESPONSES = "store-partial-responses"
}
