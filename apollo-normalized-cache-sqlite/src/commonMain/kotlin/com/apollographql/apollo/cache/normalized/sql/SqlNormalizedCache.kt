package com.apollographql.apollo.cache.normalized.sql

import com.apollographql.apollo.cache.ApolloCacheHeaders
import com.apollographql.apollo.cache.ApolloCacheHeaders.EVICT_AFTER_READ
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.NormalizedCache
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter
import com.apollographql.apollo.cache.normalized.sql.internal.RecordsetDataSource.deleteAllRecords
import com.apollographql.apollo.cache.normalized.sql.internal.RecordsetDataSource.deleteRecord
import com.apollographql.apollo.cache.normalized.sql.internal.RecordsetDataSource.selectAllRecords
import com.apollographql.apollo.cache.normalized.sql.internal.RecordsetDataSource.selectRecord
import com.apollographql.apollo.cache.normalized.sql.internal.RecordsetDataSource.selectRecords
import com.apollographql.apollo.cache.normalized.sql.internal.RecordsetDataSource.updateRecord
import com.apollographql.apollo.cache.normalized.sql.internal.RecordsetDataSource.updateRecords
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
