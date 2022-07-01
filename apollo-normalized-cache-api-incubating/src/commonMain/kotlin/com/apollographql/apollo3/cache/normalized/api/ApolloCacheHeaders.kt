package com.apollographql.apollo3.cache.normalized.api

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.annotations.ApolloInternal

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
   * The value of this header will be stored in the [Record] fields date
   */
  @ApolloExperimental
  const val DATE = "apollo-date"

  /**
   * How long to accept stale fields
   */
  @ApolloExperimental
  const val MAX_STALE = "apollo-max-stale"

  /**
   * To configure whether to store partial responses, please use the storePartialResponses() extension instead.
   */
  @Deprecated(
      message = "Used for backward compatibility with 2.x",
      level = DeprecationLevel.ERROR
  )
  @ApolloDeprecatedSince(Version.v3_0_1)
  const val STORE_PARTIAL_RESPONSES: String = ""
}
