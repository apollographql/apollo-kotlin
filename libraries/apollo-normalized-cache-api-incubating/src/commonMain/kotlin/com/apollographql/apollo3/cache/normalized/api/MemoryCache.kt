package com.apollographql.apollo3.cache.normalized.api

import com.apollographql.apollo3.cache.normalized.api.internal.ConcurrentMap
import com.apollographql.apollo3.cache.normalized.api.internal.Lock
import com.apollographql.apollo3.cache.normalized.api.internal.LruCache
import com.apollographql.apollo3.cache.normalized.api.internal.OptimisticNormalizedCache
import com.apollographql.apollo3.cache.normalized.api.internal.patternToRegex
import com.benasher44.uuid.Uuid
import kotlin.jvm.JvmOverloads
import kotlin.math.max
import kotlin.reflect.KClass

/**
 * Memory (multiplatform) cache implementation based on recently used property (LRU).
 *
 * [maxSizeBytes] - the maximum size in bytes the cache may occupy.
 * [expireAfterMillis] - after what timeout each entry in the cache treated as expired. By default there is no timeout.
 *
 * Expired entries removed from the cache only on cache miss ([loadRecord] operation) and not removed from the cache automatically
 * (there is no any sort of GC that runs in the background).
 */
class MemoryCache(
    private val nextCache: NormalizedCache? = null,
    private val maxSizeBytes: Int = Int.MAX_VALUE,
    private val expireAfterMillis: Long = -1,
) : OptimisticNormalizedCache {
  // A lock is only needed if there is a nextCache
  private val lock = nextCache?.let { Lock() }

  private val recordJournals = ConcurrentMap<String, RecordJournal>()

  private fun <T> lockWrite(block: () -> T): T {
    return lock?.write { block() } ?: block()
  }

  private fun <T> lockRead(block: () -> T): T {
    return lock?.read { block() } ?: block()
  }

  private val lruCache = LruCache<String, Record>(maxSize = maxSizeBytes, expireAfterMillis = expireAfterMillis) { key, record ->
    key.length + record.sizeInBytes
  }

  val size: Int
    get() = lockRead { lruCache.weight() }

  override fun loadRecord(key: String, cacheHeaders: CacheHeaders): Record? = lockRead {
    val record = lruCache[key]?.also {
      if (cacheHeaders.hasHeader(ApolloCacheHeaders.EVICT_AFTER_READ)) {
        lruCache.remove(key)
      }
    }

    record ?: nextCache?.loadRecord(key, cacheHeaders)?.also { nextCachedRecord ->
      lruCache[key] = nextCachedRecord
    }
  }.mergeJournalRecord(key)

  override fun loadRecords(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
    return keys.mapNotNull { key -> loadRecord(key, cacheHeaders) }
  }

  override fun clearAll() {
    lockWrite {
      lruCache.clear()
      nextCache?.clearAll()
    }
    recordJournals.clear()
  }

  override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
    return lockWrite {
      val record = lruCache.remove(cacheKey.key)

      if (cascade && record != null) {
        for (cacheReference in record.referencedFields()) {
          remove(CacheKey(cacheReference.key), true)
        }
      }

      val chainRemoved = nextCache?.remove(cacheKey, cascade) ?: false
      val journalRemoved = removeFromJournal(cacheKey, cascade)
      record != null || chainRemoved || journalRemoved
    }
  }

  private fun removeFromJournal(cacheKey: CacheKey, cascade: Boolean): Boolean {
    val recordJournal = recordJournals[cacheKey.key]
    if (recordJournal != null) {
      recordJournals.remove(cacheKey.key)
      if (cascade) {
        for (cacheReference in recordJournal.current.referencedFields()) {
          removeFromJournal(CacheKey(cacheReference.key), true)
        }
      }
    }
    return recordJournal != null
  }

  override fun remove(pattern: String): Int {
    val regex = patternToRegex(pattern)
    return lockWrite {
      var total = 0
      val keys = HashSet(lruCache.asMap().keys) // local copy to avoid concurrent modification
      keys.forEach {
        if (regex.matches(it)) {
          lruCache.remove(it)
          total++
        }
      }

      val chainRemoved = nextCache?.remove(pattern) ?: 0
      total + chainRemoved
    }
  }

  override fun merge(record: Record, cacheHeaders: CacheHeaders, recordMerger: RecordMerger): Set<String> {
    if (cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE)) {
      return emptySet()
    }
    return lockWrite {
      val oldRecord = loadRecord(record.key, cacheHeaders)
      val changedKeys = if (oldRecord == null) {
        lruCache[record.key] = record
        record.fieldKeys()
      } else {
        val (mergedRecord, changedKeys) = recordMerger.merge(existing = oldRecord, incoming = record, newDate = null)
        lruCache[record.key] = mergedRecord
        changedKeys
      }
      changedKeys + nextCache?.merge(record, cacheHeaders, recordMerger).orEmpty()
    }
  }

  override fun merge(records: Collection<Record>, cacheHeaders: CacheHeaders, recordMerger: RecordMerger): Set<String> {
    if (cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE)) {
      return emptySet()
    }
    return lockWrite { records.flatMap { record -> merge(record, cacheHeaders, recordMerger) } }.toSet()
  }

  override fun dump(): Map<KClass<*>, Map<String, Record>> {
    return lockRead {
      mapOf(OptimisticNormalizedCache::class to recordJournals.mapValues { (_, journal) -> journal.current }) +
          mapOf(this::class to lruCache.asMap().mapValues { (_, record) -> record }) +
          nextCache?.dump().orEmpty()
    }
  }

  internal fun clearCurrentCache() {
    lruCache.clear()
    recordJournals.clear()
  }

  override fun addOptimisticUpdates(recordSet: Collection<Record>): Set<String> {
    return recordSet.flatMap {
      addOptimisticUpdate(it)
    }.toSet()
  }

  override fun addOptimisticUpdate(record: Record): Set<String> {
    val journal = recordJournals[record.key]
    return if (journal == null) {
      recordJournals[record.key] = RecordJournal(record)
      record.fieldKeys()
    } else {
      journal.addPatch(record)
    }
  }

  override fun removeOptimisticUpdates(mutationId: Uuid): Set<String> {
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

      return RemovalResult(Record.changedKeys(oldRecord, current), false)
    }
  }
}

class MemoryCacheFactory @JvmOverloads constructor(
    private val maxSizeBytes: Int = Int.MAX_VALUE,
    private val expireAfterMillis: Long = -1,
) : NormalizedCacheFactory() {

  private var nextCacheFactory: NormalizedCacheFactory? = null

  fun chain(factory: NormalizedCacheFactory): MemoryCacheFactory {
    nextCacheFactory = factory
    return this
  }

  override fun create(): MemoryCache {
    return MemoryCache(
        nextCache = nextCacheFactory?.create(),
        maxSizeBytes = maxSizeBytes,
        expireAfterMillis = expireAfterMillis,
    )
  }
}
