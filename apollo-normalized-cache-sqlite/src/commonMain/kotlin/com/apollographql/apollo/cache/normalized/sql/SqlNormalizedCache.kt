package com.apollographql.apollo.cache.normalized.sql

import com.apollographql.apollo.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.cache.ApolloCacheHeaders.EVICT_AFTER_READ
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.NormalizedCache
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter
import okio.Buffer
import okio.IOException
import kotlin.reflect.KClass

class SqlNormalizedCache internal constructor(
    private val recordFieldAdapter: RecordFieldJsonAdapter,
    private val database: ApolloDatabase,
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

  override fun stream(key: String, cacheHeaders: CacheHeaders): JsonReader? {
    return try {
      cacheQueries.recordForKey(key)
          .executeAsList()
          .firstOrNull()
          ?.let {
            BufferedSourceJsonReader(Buffer().writeUtf8(it.record))
          }
    } catch (e: IOException) {
      null
    }
  }

  override fun loadRecords(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
    val records = selectRecordsForKey(keys)
    if (cacheHeaders.hasHeader(EVICT_AFTER_READ)) {
      deleteRecords(records.map { it.key })
    }
    return records
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

  override fun merge(recordCollection: Collection<Record>, cacheHeaders: CacheHeaders): Set<String> {
    lateinit var records: Set<String>
    database.transaction {
      records = super.merge(recordCollection, cacheHeaders)
    }
    return records
  }

  override fun performMerge(apolloRecord: Record, oldRecord: Record?, cacheHeaders: CacheHeaders): Set<String> {
    return if (oldRecord == null) {
      cacheQueries.insert(key = apolloRecord.key, record = recordFieldAdapter.toJson(apolloRecord.fields))
      apolloRecord.keys()
    } else {
      oldRecord.mergeWith(apolloRecord).also {
        if (it.isNotEmpty()) {
          cacheQueries.update(record = recordFieldAdapter.toJson(oldRecord.fields), key = oldRecord.key)
        }
      }
    }
  }

  fun selectRecordsForKey(keys: Collection<String>): List<Record> {
    return try {
      // sqllite has a limit of 999 named arguments.
      keys.chunked(999).flatMap { chunkedKeys ->
        cacheQueries.recordsForKeys(chunkedKeys)
            .executeAsList()
            .map {
              Record.builder(it.key)
                  .addFields(recordFieldAdapter.from(it.record)!!)
                  .build()
            }
      }
    } catch (e: IOException) {
      emptyList()
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

  fun deleteRecords(keys: Collection<String>): Boolean {
    var changes = 0L
    cacheQueries.transaction {
      cacheQueries.deleteRecords(keys)
      changes = cacheQueries.changes().executeAsOne()
    }
    return changes == keys.size.toLong()
  }

  fun createRecord(key: String, fields: String) {
    cacheQueries.insert(key = key, record = fields)
  }

  @ExperimentalStdlibApi
  override fun dump() = buildMap<KClass<*>, Map<String, Record>> {
    put(this@SqlNormalizedCache::class, cacheQueries.selectRecords().executeAsList().map {
      it.key to Record.builder(it.key)
          .addFields(recordFieldAdapter.from(it.record)!!)
          .build()
    }.toMap())
    putAll(nextCache?.dump().orEmpty())
  }
}
