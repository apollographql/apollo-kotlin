package com.apollographql.apollo3.cache.normalized.api

/**
 * Data read from the cache that can be represented as a JSON map.
 *
 * @see [toData]
 */
interface CacheData {
  fun toMap(): Map<String, Any?>
}
