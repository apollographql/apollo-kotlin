package com.apollographql.apollo.cache.normalized.api

import com.apollographql.apollo.annotations.ApolloExperimental

/**
 * A collection of cache headers that Apollo's implementations of [NormalizedCache] respect.
 */
object ApolloCacheHeaders {
  /**
   * Records from this request should not be stored in the [NormalizedCache].
   */
  const val DO_NOT_STORE = "do-not-store"


  /**
   * Records should be stored and read from the [MemoryCache] only.
   */
  const val MEMORY_CACHE_ONLY = "memory-cache-only"

  /**
   * Records from this request should be evicted after being read.
   */
  const val EVICT_AFTER_READ = "evict-after-read"

  /**
   * The Records will be stored with this date.
   */
  @ApolloExperimental
  const val DATE = "apollo-date"
}
