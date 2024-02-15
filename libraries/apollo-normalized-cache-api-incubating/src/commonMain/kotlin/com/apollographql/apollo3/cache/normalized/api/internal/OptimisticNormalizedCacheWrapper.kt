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
class OptimisticNormalizedCacheWrapper(private val wrapped: NormalizedCache) : OptimisticNormalizedCache {
  private val lock = Lock()
  private val recordJournals = mutableMapOf<String, RecordJournal>()

  override fun loadRecord(key: String, cacheHeaders: CacheHeaders): Record? {
    return lock.read {
      try {
        val nonOptimisticRecord = wrapped.loadRecord(key, cacheHeaders)
        nonOptimisticRecord.mergeJournalRecord(key)
      } catch (ignore: Exception) {
        null
      }
    }
  }

  override fun loadRecords(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
    return lock.read {
      val nonOptimisticRecords = wrapped.loadRecords(keys, cacheHeaders).associateBy { it.key }
      keys.mapNotNull { key ->
        nonOptimisticRecords[key].mergeJournalRecord(key)
      }
    }
  }

  override fun merge(record: Record, cacheHeaders: CacheHeaders, recordMerger: RecordMerger): Set<String> {
    return lock.write { wrapped.merge(record, cacheHeaders, recordMerger) }
  }

  override fun merge(records: Collection<Record>, cacheHeaders: CacheHeaders, recordMerger: RecordMerger): Set<String> {
    return lock.write { wrapped.merge(records, cacheHeaders, recordMerger) }
  }

  override fun clearAll() {
    lock.write {
      recordJournals.clear()
      wrapped.clearAll()
    }
  }

  override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
    return lock.write {
      var result: Boolean = wrapped.remove(cacheKey, cascade)

      val recordJournal = recordJournals[cacheKey.key]
      if (recordJournal != null) {
        recordJournals.remove(cacheKey.key)
        result = true
        if (cascade) {
          for (cacheReference in recordJournal.current.referencedFields()) {
            result = result || remove(CacheKey(cacheReference.key), true)
          }
        }
      }
      result
    }
  }

  override fun remove(pattern: String): Int {
    val regex = patternToRegex(pattern)
    var total = 0
    return lock.write {
      val iterator = recordJournals.iterator()
      while (iterator.hasNext()) {
        val entry = iterator.next()
        if (regex.matches(entry.key)) {
          iterator.remove()
          total++
        }
      }

      val chainRemoved = wrapped.remove(pattern)
      total + chainRemoved
    }
  }

  override fun addOptimisticUpdates(recordSet: Collection<Record>): Set<String> {
    return lock.write {
      recordSet.flatMap {
        addOptimisticUpdate(it)
      }
    }.toSet()
  }

  override fun addOptimisticUpdate(record: Record): Set<String> {
    return lock.write {
      val journal = recordJournals[record.key]
      if (journal == null) {
        recordJournals[record.key] = RecordJournal(record)
        record.fieldKeys()
      } else {
        journal.addPatch(record)
      }
    }
  }

  override fun removeOptimisticUpdates(mutationId: Uuid): Set<String> {
    return lock.write {
      val changedCacheKeys = mutableSetOf<String>()
      val iterator = recordJournals.iterator()
      while (iterator.hasNext()) {
        val entry = iterator.next()
        val result = entry.value.removePatch(mutationId)
        changedCacheKeys.addAll(result.changedKeys)
        if (result.isEmpty) {
          iterator.remove()
        }
      }
      changedCacheKeys
    }
  }

  override fun dump(): Map<KClass<*>, Map<String, Record>> {
    return lock.read {
      mapOf(this::class to recordJournals.mapValues { (_, journal) -> journal.current }) + wrapped.dump()
    }
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
