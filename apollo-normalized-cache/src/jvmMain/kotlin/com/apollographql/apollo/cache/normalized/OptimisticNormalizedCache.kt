package com.apollographql.apollo.cache.normalized

import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.cache.CacheHeaders
import com.nytimes.android.external.cache.CacheBuilder
import java.util.UUID
import kotlin.math.max
import kotlin.reflect.KClass

class OptimisticNormalizedCache : NormalizedCache() {

  private val lruCache = CacheBuilder.newBuilder().build<String, RecordJournal>()

  override fun loadRecord(key: String, cacheHeaders: CacheHeaders): Record? {
    return try {
      val nonOptimisticRecord = nextCache?.loadRecord(key, cacheHeaders)
      nonOptimisticRecord.mergeJournalRecord(key)
    } catch (ignore: Exception) {
      null
    }
  }

  override fun stream(key: String, cacheHeaders: CacheHeaders): JsonReader? {
    return try {
      // XXX: fix optimistic updates
      nextCache?.stream(key, cacheHeaders)
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

  override fun clearAll() {
    lruCache.invalidateAll()
    nextCache?.clearAll()
  }

  override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
    var result: Boolean = nextCache?.remove(cacheKey, cascade) ?: false

    val recordJournal = lruCache.getIfPresent(cacheKey.key)
    if (recordJournal != null) {
      lruCache.invalidate(cacheKey.key)
      result = true
      if (cascade) {
        for (cacheReference in recordJournal.snapshot.referencedFields()) {
          result = result && remove(CacheKey(cacheReference.key), true)
        }
      }
    }
    return result
  }

  fun mergeOptimisticUpdates(recordSet: Collection<Record>): Set<String> {
    return recordSet.flatMap {
      mergeOptimisticUpdate(it)
    }.toSet()
  }

  fun mergeOptimisticUpdate(record: Record): Set<String> {
    val journal = lruCache.getIfPresent(record.key)
    return if (journal == null) {
      lruCache.put(record.key, RecordJournal(record))
      setOf(record.key)
    } else {
      journal.commit(record)
    }
  }

  fun removeOptimisticUpdates(mutationId: UUID): Set<String> {
    val changedCacheKeys = mutableSetOf<String>()
    val removedKeys = mutableSetOf<String>()
    lruCache.asMap().forEach { (cacheKey, journal) ->
      changedCacheKeys.addAll(journal.revert(mutationId))
      if (journal.history.isEmpty()) {
        removedKeys.add(cacheKey)
      }
    }
    lruCache.invalidateAll(removedKeys)
    return changedCacheKeys
  }

  override fun performMerge(apolloRecord: Record, oldRecord: Record?, cacheHeaders: CacheHeaders): Set<String> {
    return emptySet()
  }

  @OptIn(ExperimentalStdlibApi::class)
  override fun dump() = buildMap<KClass<*>, Map<String, Record>> {
    put(
        this@OptimisticNormalizedCache::class,
        lruCache.asMap().mapValues { it.value.snapshot }
    )
    putAll(nextCache?.dump().orEmpty())
  }

  private fun Record?.mergeJournalRecord(key: String): Record? {
    val journal = lruCache.getIfPresent(key)
    return if (journal != null) {
      this?.toBuilder()?.build()?.apply {
        mergeWith(journal.snapshot)
      } ?: journal.snapshot.toBuilder().build()
    } else {
      this
    }
  }

  private class RecordJournal(mutationRecord: Record) {
    var snapshot: Record = mutationRecord.toBuilder().build()
    val history = mutableListOf<Record>(mutationRecord.toBuilder().build())

    /**
     * Commits new version of record to the history and invalidate snapshot version.
     */
    fun commit(record: Record): Set<String> {
      history.add(history.size, record.toBuilder().build())
      return snapshot.mergeWith(record)
    }

    /**
     * Lookups record by mutation id, if it's found removes it from the history and invalidates snapshot record. Snapshot record is
     * superposition of all record versions in the history.
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun revert(mutationId: UUID): Set<String> {
      val recordIndex = history.indexOfFirst { mutationId == it.mutationId }
      if (recordIndex == -1) {
        return emptySet()
      }
      return buildSet<String> {
        add(history.removeAt(recordIndex).key)

        for (i in max(0, recordIndex - 1) until history.size) {
          val record = history[i]
          if (i == max(0, recordIndex - 1)) {
            snapshot = record.toBuilder().build()
          } else {
            addAll(snapshot.mergeWith(record))
          }
        }
      }
    }
  }
}
