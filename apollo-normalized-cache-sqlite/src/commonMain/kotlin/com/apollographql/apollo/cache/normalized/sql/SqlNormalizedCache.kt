package com.apollographql.apollo.cache.normalized.sql

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
import com.apollographql.apollo.cache.normalized.sql.internal.RecordsetDataSource.updateRecords
import kotlin.reflect.KClass

class SqlNormalizedCache internal constructor(
    private val database: ApolloDatabase,
    private val recordFieldAdapter: RecordFieldJsonAdapter,
) : NormalizedCache() {

  override fun loadRecord(key: String, cacheHeaders: CacheHeaders): Record? {
    val record = database.selectRecord(
        key = key,
        recordFieldAdapter = recordFieldAdapter,
    )
    if (record != null) {
      if (cacheHeaders.hasHeader(EVICT_AFTER_READ)) {
        database.deleteRecord(
            key = key,
            cascade = false,
            recordFieldAdapter = recordFieldAdapter,
        )
      }
      return record
    }
    return nextCache?.loadRecord(key, cacheHeaders)
  }

  override fun loadRecords(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
    val records = database.selectRecords(
        keys = keys,
        recordFieldAdapter = recordFieldAdapter,
    )
    if (cacheHeaders.hasHeader(EVICT_AFTER_READ)) {
      records.forEach { record ->
        database.deleteRecord(
            key = record.key,
            cascade = false,
            recordFieldAdapter = recordFieldAdapter,
        )
      }
    }
    return records
  }

  override fun clearAll() {
    nextCache?.clearAll()
    database.deleteAllRecords()
  }

  override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
    return database.deleteRecord(
        key = cacheKey.key,
        cascade = cascade,
        recordFieldAdapter = recordFieldAdapter,
    )
  }

  override fun merge(records: Collection<Record>, cacheHeaders: CacheHeaders): Set<String> {
    return database.updateRecords(
        records = records,
        recordFieldAdapter = recordFieldAdapter,
    )
  }

  override fun performMerge(apolloRecord: Record, oldRecord: Record?, cacheHeaders: CacheHeaders): Set<String> {
    throw UnsupportedOperationException()
  }

  override fun dump(): Map<KClass<*>, Map<String, Record>> {
    return mapOf(
        this@SqlNormalizedCache::class to database.selectAllRecords(
            recordFieldAdapter = recordFieldAdapter,
        )
    ) + nextCache?.dump().orEmpty()
  }
}
