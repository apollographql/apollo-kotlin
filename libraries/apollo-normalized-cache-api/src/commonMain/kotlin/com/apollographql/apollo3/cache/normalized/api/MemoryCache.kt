package com.apollographql.apollo.cache.normalized.api

import com.apollographql.apollo.cache.normalized.api.internal.CacheLock
import com.apollographql.apollo.cache.normalized.api.internal.LruCache
import com.apollographql.apollo.mpp.currentTimeMillis
import okio.internal.commonAsUtf8ToByteArray
import kotlin.jvm.JvmOverloads
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
    private val maxSizeBytes: Int = Int.MAX_VALUE,
    private val expireAfterMillis: Long = -1,
) : NormalizedCache() {
  /**
   * A lock that is only used during read accesses on the JVM because
   * reads also write in order to:
   * - maintain the LRU order
   * - update the memory cache from the downstream caches
   *
   * write accesses are already locked by a higher level ReadWrite lock
   */
  private val lock = CacheLock()

  private val lruCache = LruCache<String, CacheEntry>(maxSize = maxSizeBytes) { key, cacheEntry ->
    key.commonAsUtf8ToByteArray().size + (cacheEntry?.sizeInBytes ?: 0)
  }

  val size: Int
    get() = lruCache.size()

  override fun loadRecord(key: String, cacheHeaders: CacheHeaders): Record? = lock.lock {
    val record = internalLoadRecord(key, cacheHeaders)
    record ?: nextCache?.loadRecord(key, cacheHeaders)?.also { nextCachedRecord ->
      lruCache[key] = CacheEntry(
          record = nextCachedRecord,
          expireAfterMillis = expireAfterMillis
      )
    }
  }

  override fun loadRecords(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> = lock.lock {
    val recordsByKey: Map<String, Record?> = keys.associateWith { key -> internalLoadRecord(key, cacheHeaders) }
    val missingKeys = recordsByKey.filterValues { it == null }.keys
    val nextCachedRecords = nextCache?.loadRecords(missingKeys, cacheHeaders).orEmpty()
    for (record in nextCachedRecords) {
      lruCache[record.key] = CacheEntry(
          record = record,
          expireAfterMillis = expireAfterMillis
      )
    }
    recordsByKey.values.filterNotNull() + nextCachedRecords
  }

  private fun internalLoadRecord(key: String, cacheHeaders: CacheHeaders): Record? {
    return lruCache[key]?.also { cacheEntry ->
      if (cacheEntry.isExpired || cacheHeaders.hasHeader(ApolloCacheHeaders.EVICT_AFTER_READ)) {
        lruCache.remove(key)
      }
    }?.takeUnless { it.isExpired }?.record
  }

  override fun clearAll() {
    lruCache.clear()
    nextCache?.clearAll()
  }

  override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
    val cacheEntry = lruCache.remove(cacheKey.key)

    if (cascade && cacheEntry != null) {
      for (cacheReference in cacheEntry.record.referencedFields()) {
        remove(CacheKey(cacheReference.key), true)
      }
    }

    val chainRemoved = nextCache?.remove(cacheKey, cascade) ?: false
    return cacheEntry != null || chainRemoved
  }

  override fun remove(pattern: String): Int {
    val regex = patternToRegex(pattern)
    var total = 0
    val keys = HashSet(lruCache.keys()) // local copy to avoid concurrent modification
    keys.forEach {
      if (regex.matches(it)) {
        lruCache.remove(it)
        total++
      }
    }

    val chainRemoved = nextCache?.remove(pattern) ?: 0
    return total + chainRemoved
  }

  override fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
    if (cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE)) {
      return emptySet()
    }

    val changedKeys = internalMerge(record, cacheHeaders)
    return changedKeys + nextCache?.merge(record, cacheHeaders).orEmpty()
  }

  override fun merge(records: Collection<Record>, cacheHeaders: CacheHeaders): Set<String> {
    if (cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE)) {
      return emptySet()
    }
    val changedKeys = records.flatMap { record -> internalMerge(record, cacheHeaders) }.toSet()
    return changedKeys + nextCache?.merge(records, cacheHeaders).orEmpty()
  }

  private fun internalMerge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
    val oldRecord = loadRecord(record.key, cacheHeaders)
    val changedKeys = if (oldRecord == null) {
      lruCache[record.key] = CacheEntry(
          record = record,
          expireAfterMillis = expireAfterMillis
      )
      record.fieldKeys()
    } else {
      val (mergedRecord, changedKeys) = oldRecord.mergeWith(record)
      lruCache[record.key] = CacheEntry(
          record = mergedRecord,
          expireAfterMillis = expireAfterMillis
      )
      changedKeys
    }
    return changedKeys
  }

  override fun dump(): Map<KClass<*>, Map<String, Record>> {
    return mapOf(
        this::class to lruCache.dump().mapValues { (_, entry) -> entry.record }
    ) + nextCache?.dump().orEmpty()
  }

  internal fun clearCurrentCache() {
    lruCache.clear()
  }

  private class CacheEntry(
      val record: Record,
      val expireAfterMillis: Long,
  ) {
    val cachedAtMillis: Long = currentTimeMillis()

    val sizeInBytes: Int = record.sizeInBytes + 8

    val isExpired: Boolean
      get() {
        return if (expireAfterMillis < 0) {
          false
        } else {
          currentTimeMillis() - cachedAtMillis >= expireAfterMillis
        }
      }
  }
}

class MemoryCacheFactory @JvmOverloads constructor(
    private val maxSizeBytes: Int = Int.MAX_VALUE,
    private val expireAfterMillis: Long = -1,
) : NormalizedCacheFactory() {

  override fun create(): MemoryCache {
    return MemoryCache(
        maxSizeBytes = maxSizeBytes,
        expireAfterMillis = expireAfterMillis,
    )
  }
}
