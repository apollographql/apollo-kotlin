package com.apollographql.apollo.cache.normalized.sql

import com.apollographql.apollo.cache.ApolloCacheHeaders.EVICT_AFTER_READ
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.NormalizedCache
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter
import okio.IOException

class SqlNormalizedCache internal constructor(
    private val recordFieldAdapter: RecordFieldJsonAdapter,
    private val cacheQueries: CacheQueries
) : NormalizedCache() {

  override fun loadRecord(key: String, cacheHeaders: CacheHeaders): Record? {
    val record = selectRecordForKey(key)

    if (record != null) {
      if (cacheHeaders.hasHeader(EVICT_AFTER_READ)) {
        deleteRecord(key)
      }
      return record
    }
    return nextCache?.loadRecord(key, cacheHeaders)
  }

  override fun clearAll() {
    nextCache?.clearAll()
    cacheQueries.deleteAll()
  }

  override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
    val result: Boolean = nextCache?.remove(cacheKey, cascade) ?: false
    if (result) {
      return true
    }
    return if (cascade) {
      selectRecordForKey(cacheKey.key)
          ?.referencedFields()
          ?.all { remove(CacheKey(it.key), cascade = true) }
          ?: false
    } else {
      deleteRecord(cacheKey.key)
    }
  }

  override fun performMerge(apolloRecord: Record, cacheHeaders: CacheHeaders): Set<String> {
    val oldRecord = selectRecordForKey(apolloRecord.key)
    return if (oldRecord == null) {
      cacheQueries.insert(key = apolloRecord.key, record = recordFieldAdapter.toJson(apolloRecord.fields))
      emptySet()
    } else {
      oldRecord.mergeWith(apolloRecord).also {
        if (it.isNotEmpty()) {
          cacheQueries.update(record = recordFieldAdapter.toJson(oldRecord.fields), key = oldRecord.key)
        }
      }
    }
  }

  fun selectRecordForKey(key: String): Record? {
    return try {
      cacheQueries.recordForKey(key)
          .executeAsList()
          .firstOrNull()
          ?.let {
            Record.builder(it.key)
                .addFields(recordFieldAdapter.from(it.record)!!)
                .build()
          }
    } catch (e: IOException) {
      null
    }
  }

  private inline fun <T> Collection<T>.all(predicate: (T) -> Boolean): Boolean {
    var result = true
    for (element in this) {
      result = result && predicate(element)
    }
    return result
  }

  fun deleteRecord(key: String): Boolean {
    var changes = 0L
    cacheQueries.transaction {
      cacheQueries.delete(key)
      changes = cacheQueries.changes().executeAsOne()
    }
    return changes > 0
  }

  fun createRecord(key: String, fields: String) {
    cacheQueries.insert(key = key, record = fields)
  }
}
