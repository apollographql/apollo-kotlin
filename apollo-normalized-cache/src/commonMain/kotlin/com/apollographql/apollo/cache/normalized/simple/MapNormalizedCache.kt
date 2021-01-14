package com.apollographql.apollo.cache.normalized.simple

import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.NormalizedCache
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.internal.MapJsonReader

/**
 * A simple normalized cache backed by a [MutableMap].
 *
 * A [MapNormalizedCache] keeps its entry in memory forever and can only grow. Do not use it to store big amounts of data.
 */
class MapNormalizedCache : NormalizedCache() {
  private val map = mutableMapOf<String, Record>()

  override fun loadRecord(key: String, cacheHeaders: CacheHeaders): Record? {
    val record = nextCache?.loadRecord(key, cacheHeaders)
    if (record != null) {
      return record
    }

    return map.get(key)
  }

  override fun stream(key: String, cacheHeaders: CacheHeaders): JsonReader? {
    return map.get(key)?.let { MapJsonReader(it) }
  }

  override fun performMerge(apolloRecord: Record, oldRecord: Record?, cacheHeaders: CacheHeaders): Set<String> {
    return map.getOrPut(apolloRecord.key, {apolloRecord})
        .mergeWith(apolloRecord)
  }

  override fun clearAll() {
    nextCache?.clearAll()
    map.clear()
  }

  override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
    var result: Boolean = nextCache?.remove(cacheKey, cascade) ?: false

    val record = map.get(cacheKey.key)
    if (record != null) {
      map.remove(cacheKey.key)
      result = true
      if (cascade) {
        for (cacheReference in record.referencedFields()) {
          result = result && remove(CacheKey(cacheReference.key), true)
        }
      }
    }
    return result
  }
}