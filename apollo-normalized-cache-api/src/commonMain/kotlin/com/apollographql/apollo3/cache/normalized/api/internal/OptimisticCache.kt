package com.apollographql.apollo3.cache.normalized.api.internal

import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.NormalizedCache
import com.apollographql.apollo3.cache.normalized.api.Record
import com.benasher44.uuid.Uuid
import kotlin.reflect.KClass

class OptimisticCache : NormalizedCache() {
  private val lruCache = LruCache<String, RecordJournal>(maxSize = Int.MAX_VALUE)

  override fun loadRecord(key: String, cacheHeaders: CacheHeaders): Record? {
    return try {
      val nonOptimisticRecord = nextCache?.loadRecord(key, cacheHeaders)
      nonOptimisticRecord.mergeJournalRecord(key)
    } catch (ignore: Exception) {
      null
    }
  }

  override fun loadRecords(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
    val nonOptimisticRecords = nextCache?.loadRecords(keys, cacheHeaders)?.associateBy { it.key } ?: emptyMap()
    return keys.mapNotNull { key ->
      nonOptimisticRecords[key].mergeJournalRecord(key)
    }
  }

  override fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
    return nextCache?.merge(record, cacheHeaders) ?: emptySet()
  }

  override fun merge(records: Collection<Record>, cacheHeaders: CacheHeaders): Set<String> {
    return nextCache?.merge(records, cacheHeaders) ?: emptySet()
  }

  override fun clearAll() {
    lruCache.clear()
    nextCache?.clearAll()
  }

  override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
    var result: Boolean = nextCache?.remove(cacheKey, cascade) ?: false

    val recordJournal = lruCache[cacheKey.key]
    if (recordJournal != null) {
      lruCache.remove(cacheKey.key)
      result = true
      if (cascade) {
        for (cacheReference in recordJournal.snapshot.referencedFields()) {
          result = result && remove(CacheKey(cacheReference.key), true)
        }
      }
    }
    return result
  }

  override fun remove(pattern: String): Int {
    val regex = patternToRegex(pattern)
    var total = 0
    lruCache.keys().forEach {
      if (regex.matches(it)){
        lruCache.remove(it)
        total++
      }
    }

    val chainRemoved = nextCache?.remove(pattern) ?: 0
    return total + chainRemoved
  }

  fun mergeOptimisticUpdates(recordSet: Collection<Record>): Set<String> {
    return recordSet.flatMap {
      mergeOptimisticUpdate(it)
    }.toSet()
  }

  fun mergeOptimisticUpdate(record: Record): Set<String> {
    val journal = lruCache[record.key]
    return if (journal == null) {
      lruCache[record.key] = RecordJournal(record)
      setOf(record.key)
    } else {
      journal.commit(record)
    }
  }

  fun removeOptimisticUpdates(mutationId: Uuid): Set<String> {
    val changedCacheKeys = mutableSetOf<String>()
    val removedKeys = mutableSetOf<String>()
    lruCache.dump().forEach { (cacheKey, journal) ->
      changedCacheKeys.addAll(journal.revert(mutationId))
      if (journal.history.isEmpty()) {
        removedKeys.add(cacheKey)
      }
    }
    lruCache.remove(removedKeys)
    return changedCacheKeys
  }

  override fun dump(): Map<KClass<*>, Map<String, Record>> {
    return mapOf(
        this::class to lruCache.dump().mapValues { (_, journal) -> journal.snapshot }
    ) + nextCache?.dump().orEmpty()
  }

  private fun Record?.mergeJournalRecord(key: String): Record? {
    val journal = lruCache[key]
    return if (journal != null) {
      this?.mergeWith(journal.snapshot)?.first ?: journal.snapshot
    } else {
      this
    }
  }

  private class RecordJournal(mutationRecord: Record) {
    var snapshot: Record = mutationRecord
    val history = mutableListOf(mutationRecord)

    /**
     * Commits new version of record to the history and invalidate snapshot version.
     */
    fun commit(record: Record): Set<String> {
      val (mergedRecord, changedKeys) = snapshot.mergeWith(record)
      snapshot = mergedRecord
      history.add(history.size, mergedRecord)
      return changedKeys
    }

    /**
     * Lookups record by mutation id, if it's found removes it from the history and invalidates snapshot record. Snapshot record is
     * superposition of all record versions in the history.
     */
    fun revert(mutationId: Uuid): Set<String> {
      val recordIndex = history.indexOfFirst { mutationId == it.mutationId }
      if (recordIndex == -1) {
        return emptySet()
      }

      val result = HashSet<String>()
      result.add(history.removeAt(recordIndex).key)

      for (i in kotlin.math.max(0, recordIndex - 1) until history.size) {
        val record = history[i]
        if (i == kotlin.math.max(0, recordIndex - 1)) {
          snapshot = record
        } else {
          val (mergedRecord, changedKeys) = snapshot.mergeWith(record)
          snapshot = mergedRecord
          result.addAll(changedKeys)
        }
      }

      return result
    }
  }
}
