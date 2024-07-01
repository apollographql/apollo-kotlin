package com.apollographql.apollo.cache.normalized.api

import com.apollographql.apollo.annotations.ApolloInternal

/**
 * Data read from the cache that can be represented as a JSON map.
 *
 * @see [toData]
 */
@ApolloInternal
interface CacheData {
  fun toMap(): Map<String, Any?>
}
