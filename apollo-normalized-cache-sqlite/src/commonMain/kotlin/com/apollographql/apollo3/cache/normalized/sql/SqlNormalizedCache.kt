package com.apollographql.apollo3.cache.normalized.sql

import com.apollographql.apollo3.cache.normalized.api.ApolloCacheHeaders
import com.apollographql.apollo3.cache.normalized.api.ApolloCacheHeaders.EVICT_AFTER_READ
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.NormalizedCache
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.cache.normalized.sql.internal.CacheQueriesHelpers.deleteAllRecords
import com.apollographql.apollo3.cache.normalized.sql.internal.CacheQueriesHelpers.deleteRecord
import com.apollographql.apollo3.cache.normalized.sql.internal.CacheQueriesHelpers.remove
import com.apollographql.apollo3.cache.normalized.sql.internal.CacheQueriesHelpers.selectAllRecords
import com.apollographql.apollo3.cache.normalized.sql.internal.CacheQueriesHelpers.selectRecord
import com.apollographql.apollo3.cache.normalized.sql.internal.CacheQueriesHelpers.selectRecords
import com.apollographql.apollo3.cache.normalized.sql.internal.CacheQueriesHelpers.updateRecord
import com.apollographql.apollo3.cache.normalized.sql.internal.CacheQueriesHelpers.updateRecords
import kotlin.reflect.KClass

class SqlNormalizedCache internal constructor(
    private val cacheQueries: CacheQueries,
) : NormalizedCache() {

  override fun loadRecord(key: String, cacheHeaders: CacheHeaders): Record? {
    val record = cacheQueries.selectRecord(key)
    if (record != null) {
      if (cacheHeaders.hasHeader(EVICT_AFTER_READ)) {
        cacheQueries.deleteRecord(
            key = key,
            cascade = false,
        )
      }
      return record
    }
    return nextCache?.loadRecord(key, cacheHeaders)
  }

  override fun loadRecords(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
    val records = cacheQueries.selectRecords(keys)
    if (cacheHeaders.hasHeader(EVICT_AFTER_READ)) {
      records.forEach { record ->
        cacheQueries.deleteRecord(
            key = record.key,
            cascade = false,
        )
      }
    }
    return records
  }

  override fun clearAll() {
    nextCache?.clearAll()
    cacheQueries.deleteAllRecords()
  }

  override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
    return cacheQueries.deleteRecord(
        key = cacheKey.key,
        cascade = cascade,
    )
  }

  override fun remove(pattern: String): Int {
    val selfRemoved = cacheQueries.remove(pattern)
    val chainRemoved = nextCache?.remove(pattern) ?: 0

    return selfRemoved + chainRemoved
  }

  override fun merge(records: Collection<Record>, cacheHeaders: CacheHeaders): Set<String> {
    if (cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE)) {
      return emptySet()
    }
    return cacheQueries.updateRecords(
        records = records,
    )
  }

  override fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
    if (cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE)) {
      return emptySet()
    }
    return cacheQueries.updateRecord(
        record = record,
    )
  }

  override fun dump(): Map<KClass<*>, Map<String, Record>> {
    return mapOf(
        this@SqlNormalizedCache::class to cacheQueries.selectAllRecords()
    ) + nextCache?.dump().orEmpty()
  }
}
