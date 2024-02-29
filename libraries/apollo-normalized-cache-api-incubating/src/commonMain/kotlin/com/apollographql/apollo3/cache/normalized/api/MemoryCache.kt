package com.apollographql.apollo3.cache.normalized.api

import com.apollographql.apollo3.cache.normalized.api.internal.Lock
import com.apollographql.apollo3.cache.normalized.api.internal.LruCache
import com.apollographql.apollo3.cache.normalized.api.internal.patternToRegex
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
    private val nextCache: NormalizedCache? = null,
    private val maxSizeBytes: Int = Int.MAX_VALUE,
    private val expireAfterMillis: Long = -1,
) : NormalizedCache {
  // A lock is only needed if there is a nextCache
  private val lock = nextCache?.let { Lock() }

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
  }

  override fun loadRecords(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
    return keys.mapNotNull { key -> loadRecord(key, cacheHeaders) }
  }

  override fun clearAll() {
    lockWrite {
      lruCache.clear()
      nextCache?.clearAll()
    }
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
      record != null || chainRemoved
    }
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
      mapOf(this::class to lruCache.asMap().mapValues { (_, record) -> record }) +
          nextCache?.dump().orEmpty()
    }
  }

  internal fun clearCurrentCache() {
    lruCache.clear()
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
