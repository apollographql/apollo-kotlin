package com.apollographql.apollo.cache.normalized.sql

import com.apollographql.apollo.cache.normalized.api.ApolloCacheHeaders
import com.apollographql.apollo.cache.normalized.api.ApolloCacheHeaders.EVICT_AFTER_READ
import com.apollographql.apollo.cache.normalized.api.CacheHeaders
import com.apollographql.apollo.cache.normalized.api.CacheKey
import com.apollographql.apollo.cache.normalized.api.NormalizedCache
import com.apollographql.apollo.cache.normalized.api.Record
import com.apollographql.apollo.cache.normalized.sql.internal.RecordDatabase
import com.apollographql.apollo.exception.apolloExceptionHandler
import kotlin.reflect.KClass

class SqlNormalizedCache internal constructor(
    private val recordDatabase: RecordDatabase,
) : NormalizedCache() {

  override fun loadRecord(key: String, cacheHeaders: CacheHeaders): Record? {
    if (cacheHeaders.hasHeader(ApolloCacheHeaders.MEMORY_CACHE_ONLY)) {
      return null
    }
    val record = try {
      recordDatabase.select(key)
    } catch (e: Exception) {
      // Unable to read the record from the database, it is possibly corrupted - treat this as a cache miss
      apolloExceptionHandler(Exception("Unable to read a record from the database", e))
      null
    }
    if (record != null) {
      if (cacheHeaders.hasHeader(EVICT_AFTER_READ)) {
        recordDatabase.delete(key)
      }
      return record
    }
    return nextCache?.loadRecord(key, cacheHeaders)
  }

  override fun loadRecords(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
    if (cacheHeaders.hasHeader(ApolloCacheHeaders.MEMORY_CACHE_ONLY)) {
      return emptyList()
    }
    val records = try {
      internalGetRecords(keys)
    } catch (e: Exception) {
      // Unable to read the records from the database, it is possibly corrupted - treat this as a cache miss
      apolloExceptionHandler(Exception("Unable to read records from the database", e))
      emptyList()
    }
    if (cacheHeaders.hasHeader(EVICT_AFTER_READ)) {
      records.forEach { record ->
        recordDatabase.delete(record.key)
      }
    }
    val missRecordKeys = keys - records.map { it.key }.toSet()
    val missRecords = missRecordKeys.ifEmpty { null }?.let { nextCache?.loadRecords(it, cacheHeaders) }.orEmpty()
    return records + missRecords
  }

  override fun clearAll() {
    nextCache?.clearAll()
    recordDatabase.deleteAll()
  }

  override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
    val selfRemoved = recordDatabase.transaction {
      internalDeleteRecord(
          key = cacheKey.key,
          cascade = cascade,
      )
    }
    val chainRemoved = nextCache?.remove(cacheKey, cascade) ?: false
    return selfRemoved || chainRemoved
  }

  override fun remove(pattern: String): Int {
    var selfRemoved = 0
    recordDatabase.transaction {
      recordDatabase.deleteMatching(pattern)
      selfRemoved = recordDatabase.changes().toInt()
    }
    val chainRemoved = nextCache?.remove(pattern) ?: 0

    return selfRemoved + chainRemoved
  }

  override fun merge(records: Collection<Record>, cacheHeaders: CacheHeaders): Set<String> {
    if (cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE) || cacheHeaders.hasHeader(ApolloCacheHeaders.MEMORY_CACHE_ONLY)) {
      return emptySet()
    }
    return try {
      internalUpdateRecords(records = records, cacheHeaders.date()) + nextCache?.merge(records, cacheHeaders).orEmpty()
    } catch (e: Exception) {
      // Unable to merge the records in the database, it is possibly corrupted - treat this as a cache miss
      apolloExceptionHandler(Exception("Unable to merge records from the database", e))
      emptySet()
    }
  }

  private fun CacheHeaders.date(): Long? {
    return headerValue(ApolloCacheHeaders.DATE)?.toLong()
  }

  override fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
    if (cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE) || cacheHeaders.hasHeader(ApolloCacheHeaders.MEMORY_CACHE_ONLY)) {
      return emptySet()
    }
    return try {
      internalUpdateRecord(record, cacheHeaders.date()) + nextCache?.merge(record, cacheHeaders).orEmpty()
    } catch (e: Exception) {
      // Unable to merge the record in the database, it is possibly corrupted - treat this as a cache miss
      apolloExceptionHandler(Exception("Unable to merge a record from the database", e))
      emptySet()
    }
  }

  override fun dump(): Map<KClass<*>, Map<String, Record>> {
    return mapOf(
        this@SqlNormalizedCache::class to recordDatabase.selectAll().associateBy { it.key }
    ) + nextCache?.dump().orEmpty()
  }

  /**
   * Assume an enclosing transaction
   */
  private fun internalDeleteRecord(key: String, cascade: Boolean, visited: MutableSet<String> = mutableSetOf()): Boolean {
    if (cascade) {
      // If we've already visited this key, return to prevent infinite loop
      if (key in visited) return false
      visited.add(key)

      recordDatabase.select(key)
          ?.referencedFields()
          ?.forEach {
            internalDeleteRecord(
                key = it.key,
                cascade = true,
                visited = visited
            )
          }
    }
    recordDatabase.delete(key)
    return recordDatabase.changes() > 0
  }

  /**
   * Update records, loading the previous ones
   *
   * This is an optimization over [internalUpdateRecord]
   */
  private fun internalUpdateRecords(records: Collection<Record>, date: Long?): Set<String> {
    var updatedRecordKeys: Set<String> = emptySet()
    recordDatabase.transaction {
      val oldRecords = internalGetRecords(
          keys = records.map { it.key },
      ).associateBy { it.key }

      updatedRecordKeys = records.flatMap { record ->
        val oldRecord = oldRecords[record.key]
        if (oldRecord == null) {
          recordDatabase.insert(record.withDate(date))
          record.fieldKeys()
        } else {
          val (mergedRecord, changedKeys) = oldRecord.mergeWith(record, date)
          if (mergedRecord.isNotEmpty()) {
            recordDatabase.update(mergedRecord)
          }
          changedKeys
        }
      }.toSet()
    }
    return updatedRecordKeys
  }

  private fun Record.withDate(date: Long?): Record {
    if (date == null) {
      return this
    }
    return Record(
        key,
        fields,
        mutationId,
        fields.mapValues { date }
    )

  }

  /**
   * Update a single [Record], loading the previous one
   */
  private fun internalUpdateRecord(record: Record, date: Long?): Set<String> {
    return recordDatabase.transaction {
      val oldRecord = recordDatabase.select(record.key)

      if (oldRecord == null) {
        recordDatabase.insert(record.withDate(date))
        record.fieldKeys()
      } else {
        val (mergedRecord, changedKeys) = oldRecord.mergeWith(record, date)
        if (mergedRecord.isNotEmpty()) {
          recordDatabase.update(mergedRecord)
        }
        changedKeys
      }
    }
  }

  /**
   * Loads a list of records, making sure to not query more than 999 at a time
   * to help with the SQLite limitations
   */
  private fun internalGetRecords(keys: Collection<String>): List<Record> {
    return keys.chunked(999).flatMap { chunkedKeys ->
      recordDatabase.select(chunkedKeys)
    }
  }
}
