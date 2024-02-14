package com.apollographql.apollo3.cache.normalized.sql

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.cache.normalized.api.ApolloCacheHeaders
import com.apollographql.apollo3.cache.normalized.api.ApolloCacheHeaders.EVICT_AFTER_READ
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.NormalizedCache
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.cache.normalized.api.RecordMerger
import com.apollographql.apollo3.cache.normalized.sql.internal.RecordDatabase
import com.apollographql.apollo3.exception.apolloExceptionHandler
import kotlin.reflect.KClass

class SqlNormalizedCache internal constructor(
    private val recordDatabase: RecordDatabase,
) : NormalizedCache {

  private fun <T> maybeTransaction(condition: Boolean, block: () -> T): T {
    return if (condition) {
      recordDatabase.transaction {
        block()
      }
    } else {
      block()
    }
  }

  override fun loadRecord(key: String, cacheHeaders: CacheHeaders): Record? {
    val evictAfterRead = cacheHeaders.hasHeader(EVICT_AFTER_READ)
    return maybeTransaction(evictAfterRead) {
      try {
        recordDatabase.select(key)
      } catch (e: Exception) {
        // Unable to read the record from the database, it is possibly corrupted - treat this as a cache miss
        apolloExceptionHandler(Exception("Unable to read a record from the database", e))
        null
      }?.also {
        if (evictAfterRead) {
          recordDatabase.delete(key)
        }
      }
    }
  }

  override fun loadRecords(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
    val evictAfterRead = cacheHeaders.hasHeader(EVICT_AFTER_READ)
    return maybeTransaction(evictAfterRead) {
      try {
        internalGetRecords(keys)
      } catch (e: Exception) {
        // Unable to read the records from the database, it is possibly corrupted - treat this as a cache miss
        apolloExceptionHandler(Exception("Unable to read records from the database", e))
        emptyList()
      }.also {
        if (evictAfterRead) {
          it.forEach { record ->
            recordDatabase.delete(record.key)
          }
        }
      }
    }
  }

  override fun clearAll() {
    recordDatabase.deleteAll()
  }

  override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
    return recordDatabase.transaction {
      internalDeleteRecord(
          key = cacheKey.key,
          cascade = cascade,
      )
    }
  }

  override fun remove(pattern: String): Int {
    return recordDatabase.transaction {
      recordDatabase.deleteMatching(pattern)
      recordDatabase.changes().toInt()
    }
  }

  private fun CacheHeaders.date(): Long? {
    return headerValue(ApolloCacheHeaders.DATE)?.toLong()
  }

  @ApolloExperimental
  override fun merge(record: Record, cacheHeaders: CacheHeaders, recordMerger: RecordMerger): Set<String> {
    if (cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE)) {
      return emptySet()
    }
    return try {
      internalUpdateRecord(record = record, recordMerger = recordMerger, date = cacheHeaders.date())
    } catch (e: Exception) {
      // Unable to merge the record in the database, it is possibly corrupted - treat this as a cache miss
      apolloExceptionHandler(Exception("Unable to merge a record from the database", e))
      emptySet()
    }
  }

  @ApolloExperimental
  override fun merge(records: Collection<Record>, cacheHeaders: CacheHeaders, recordMerger: RecordMerger): Set<String> {
    if (cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE)) {
      return emptySet()
    }
    return try {
      internalUpdateRecords(records = records, recordMerger = recordMerger, date = cacheHeaders.date())
    } catch (e: Exception) {
      // Unable to merge the records in the database, it is possibly corrupted - treat this as a cache miss
      apolloExceptionHandler(Exception("Unable to merge records from the database", e))
      emptySet()
    }
  }

  override fun dump(): Map<KClass<*>, Map<String, Record>> {
    return mapOf(this::class to recordDatabase.selectAll().associateBy { it.key })
  }

  /**
   * Assume an enclosing transaction
   */
  private fun internalDeleteRecord(key: String, cascade: Boolean): Boolean {
    if (cascade) {
      recordDatabase.select(key)
          ?.referencedFields()
          ?.forEach {
            internalDeleteRecord(
                key = it.key,
                cascade = true,
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
  private fun internalUpdateRecords(records: Collection<Record>, recordMerger: RecordMerger, date: Long?): Set<String> {
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
          val (mergedRecord, changedKeys) = recordMerger.merge(existing = oldRecord, incoming = record, newDate = date)
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
        key = key,
        fields = fields,
        mutationId = mutationId,
        date = fields.mapValues { date },
        metadata = metadata
    )
  }

  /**
   * Update a single [Record], loading the previous one
   */
  private fun internalUpdateRecord(record: Record, recordMerger: RecordMerger, date: Long?): Set<String> {
    return recordDatabase.transaction {
      val oldRecord = recordDatabase.select(record.key)

      if (oldRecord == null) {
        recordDatabase.insert(record.withDate(date))
        record.fieldKeys()
      } else {
        val (mergedRecord, changedKeys) = recordMerger.merge(existing = oldRecord, incoming = record, newDate = date)
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
