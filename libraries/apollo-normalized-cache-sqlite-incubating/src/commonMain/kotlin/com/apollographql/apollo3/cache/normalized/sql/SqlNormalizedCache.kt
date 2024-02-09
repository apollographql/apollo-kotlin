package com.apollographql.apollo3.cache.normalized.sql

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.cache.normalized.api.ApolloCacheHeaders
import com.apollographql.apollo3.cache.normalized.api.ApolloCacheHeaders.EVICT_AFTER_READ
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.DefaultRecordMerger
import com.apollographql.apollo3.cache.normalized.api.NormalizedCache
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.cache.normalized.api.RecordMerger
import com.apollographql.apollo3.cache.normalized.api.internal.Lock
import com.apollographql.apollo3.cache.normalized.sql.internal.RecordDatabase
import com.apollographql.apollo3.exception.apolloExceptionHandler
import kotlin.reflect.KClass

class SqlNormalizedCache internal constructor(
    private val recordDatabase: RecordDatabase,
) : NormalizedCache() {

  // A lock is only needed if there is a nextCache
  private val lock = nextCache?.let { Lock() }

  private fun <T> lockWrite(block: () -> T): T {
    return lock?.write { block() } ?: block()
  }

  private fun <T> lockRead(block: () -> T): T {
    return lock?.read { block() } ?: block()
  }

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
    return lockWrite {
      maybeTransaction(evictAfterRead) {
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
      } ?: nextCache?.loadRecord(key, cacheHeaders)
    }
  }

  override fun loadRecords(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
    val evictAfterRead = cacheHeaders.hasHeader(EVICT_AFTER_READ)
    return lockWrite {
      val records = maybeTransaction(evictAfterRead) {
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
      val missRecordKeys = keys - records.map { it.key }.toSet()
      val missRecords = missRecordKeys.ifEmpty { null }?.let { nextCache?.loadRecords(it, cacheHeaders) }.orEmpty()
      records + missRecords
    }
  }

  override fun clearAll() {
    lockWrite {
      nextCache?.clearAll()
      recordDatabase.deleteAll()
    }
  }

  override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
    return lockWrite {
      val selfRemoved = recordDatabase.transaction {
        internalDeleteRecord(
            key = cacheKey.key,
            cascade = cascade,
        )
      }
      val chainRemoved = nextCache?.remove(cacheKey, cascade) ?: false
      selfRemoved || chainRemoved
    }
  }

  override fun remove(pattern: String): Int {
    return lockWrite {
      var selfRemoved = 0
      recordDatabase.transaction {
        recordDatabase.deleteMatching(pattern)
        selfRemoved = recordDatabase.changes().toInt()
      }
      val chainRemoved = nextCache?.remove(pattern) ?: 0

      selfRemoved + chainRemoved
    }
  }

  private fun CacheHeaders.date(): Long? {
    return headerValue(ApolloCacheHeaders.DATE)?.toLong()
  }

  override fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
    return merge(record, cacheHeaders, DefaultRecordMerger)
  }

  override fun merge(records: Collection<Record>, cacheHeaders: CacheHeaders): Set<String> {
    return merge(records, cacheHeaders, DefaultRecordMerger)
  }

  @ApolloExperimental
  override fun merge(record: Record, cacheHeaders: CacheHeaders, recordMerger: RecordMerger): Set<String> {
    if (cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE)) {
      return emptySet()
    }
    return lockWrite {
      try {
        internalUpdateRecord(record = record, recordMerger = recordMerger, date = cacheHeaders.date())
      } catch (e: Exception) {
        // Unable to merge the record in the database, it is possibly corrupted - treat this as a cache miss
        apolloExceptionHandler(Exception("Unable to merge a record from the database", e))
        emptySet()
      } + nextCache?.merge(record, cacheHeaders).orEmpty()
    }
  }

  @ApolloExperimental
  override fun merge(records: Collection<Record>, cacheHeaders: CacheHeaders, recordMerger: RecordMerger): Set<String> {
    if (cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE)) {
      return emptySet()
    }
    return lockWrite {
      try {
        internalUpdateRecords(records = records, recordMerger = recordMerger, date = cacheHeaders.date())
      } catch (e: Exception) {
        // Unable to merge the records in the database, it is possibly corrupted - treat this as a cache miss
        apolloExceptionHandler(Exception("Unable to merge records from the database", e))
        emptySet()
      } + nextCache?.merge(records, cacheHeaders).orEmpty()
    }
  }

  override fun dump(): Map<KClass<*>, Map<String, Record>> {
    return lockRead {
      mapOf(this::class to recordDatabase.selectAll().associateBy { it.key }) + nextCache?.dump().orEmpty()
    }
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
