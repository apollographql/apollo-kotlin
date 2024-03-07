package com.apollographql.apollo3.cache.normalized.api.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.NormalizedCache
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.cache.normalized.api.Record.Companion.changedKeys
import com.apollographql.apollo3.cache.normalized.api.RecordMerger
import com.benasher44.uuid.Uuid
import kotlin.math.max
import kotlin.reflect.KClass

@ApolloInternal
class OptimisticNormalizedCache(private val wrapped: NormalizedCache) : NormalizedCache {
  private val recordJournals = ConcurrentMap<String, RecordJournal>()

  override fun loadRecord(key: String, cacheHeaders: CacheHeaders): Record? {
    val nonOptimisticRecord = wrapped.loadRecord(key, cacheHeaders)
    return nonOptimisticRecord.mergeJournalRecord(key)
  }

  override fun loadRecords(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
    val nonOptimisticRecords = wrapped.loadRecords(keys, cacheHeaders).associateBy { it.key }
    return keys.mapNotNull { key ->
      nonOptimisticRecords[key].mergeJournalRecord(key)
    }
  }

  override fun merge(record: Record, cacheHeaders: CacheHeaders, recordMerger: RecordMerger): Set<String> {
    return wrapped.merge(record, cacheHeaders, recordMerger)
  }

  override fun merge(records: Collection<Record>, cacheHeaders: CacheHeaders, recordMerger: RecordMerger): Set<String> {
    return wrapped.merge(records, cacheHeaders, recordMerger)
  }

  override fun clearAll() {
    wrapped.clearAll()
    recordJournals.clear()
  }

  override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
    var removed = wrapped.remove(cacheKey, cascade)

    val recordJournal = recordJournals[cacheKey.key]
    if (recordJournal != null) {
      recordJournals.remove(cacheKey.key)
      removed = true
      if (cascade) {
        for (cacheReference in recordJournal.current.referencedFields()) {
          remove(CacheKey(cacheReference.key), true)
        }
      }
    }
    return removed
  }

  override fun remove(pattern: String): Int {
    var removed = wrapped.remove(pattern)

    val regex = patternToRegex(pattern)
    val keys = HashSet(recordJournals.keys) // local copy to avoid concurrent modification
    keys.forEach { key ->
      if (regex.matches(key)) {
        recordJournals.remove(key)
        removed++
      }
    }

    return removed
  }

  fun addOptimisticUpdates(recordSet: Collection<Record>): Set<String> {
    return recordSet.flatMap {
      addOptimisticUpdate(it)
    }.toSet()
  }

  fun addOptimisticUpdate(record: Record): Set<String> {
    val journal = recordJournals[record.key]
    return if (journal == null) {
      recordJournals[record.key] = RecordJournal(record)
      record.fieldKeys()
    } else {
      journal.addPatch(record)
    }
  }

  fun removeOptimisticUpdates(mutationId: Uuid): Set<String> {
    val changedCacheKeys = mutableSetOf<String>()
    val keys = HashSet(recordJournals.keys) // local copy to avoid concurrent modification
    keys.forEach {
      val recordJournal = recordJournals[it] ?: return@forEach
      val result = recordJournal.removePatch(mutationId)
      changedCacheKeys.addAll(result.changedKeys)
      if (result.isEmpty) {
        recordJournals.remove(it)
      }
    }
    return changedCacheKeys
  }

  override fun dump(): Map<KClass<*>, Map<String, Record>> {
    return mapOf(this::class to recordJournals.mapValues { (_, journal) -> journal.current }) + wrapped.dump()
  }

  private fun Record?.mergeJournalRecord(key: String): Record? {
    val journal = recordJournals[key]
    return if (journal != null) {
      this?.mergeWith(journal.current)?.first ?: journal.current
    } else {
      this
    }
  }

  private class RemovalResult(
      val changedKeys: Set<String>,
      val isEmpty: Boolean,
  )

  private class RecordJournal(record: Record) {
    /**
     * The latest value of the record made by applying all the patches.
     */
    var current: Record = record

    /**
     * A list of chronological patches applied to the record.
     */
    private val patches = mutableListOf(record)

    /**
     * Adds a new patch on top of all the previous ones.
     */
    fun addPatch(record: Record): Set<String> {
      val (mergedRecord, changedKeys) = current.mergeWith(record)
      current = mergedRecord
      patches.add(record)
      return changedKeys
    }

    /**
     * Lookup record by mutation id, if it's found removes it from the history and
     * computes the new current record.
     *
     * @return the changed keys or null if
     */
    fun removePatch(mutationId: Uuid): RemovalResult {
      val recordIndex = patches.indexOfFirst { mutationId == it.mutationId }
      if (recordIndex == -1) {
        // The mutation did not impact this Record
        return RemovalResult(emptySet(), false)
      }

      if (patches.size == 1) {
        // The mutation impacted this Record and it was the only one in the history
        return RemovalResult(current.fieldKeys(), true)
      }

      /**
       * There are multiple patches, go over them and compute the new current value
       * Remember the oldRecord so that we can compute the changed keys
       */
      val oldRecord = current

      patches.removeAt(recordIndex).key

      var cur: Record? = null
      val start = max(0, recordIndex - 1)
      for (i in start until patches.size) {
        val record = patches[i]
        if (cur == null) {
          cur = record
        } else {
          val (mergedRecord, _) = cur.mergeWith(record)
          cur = mergedRecord
        }
      }
      current = cur!!

      return RemovalResult(changedKeys(oldRecord, current), false)
    }
  }
}
