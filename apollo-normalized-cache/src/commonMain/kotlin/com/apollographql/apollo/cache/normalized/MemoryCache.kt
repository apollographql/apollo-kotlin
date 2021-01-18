package com.apollographql.apollo.cache.normalized

import com.apollographql.apollo.cache.ApolloCacheHeaders
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.internal.LruCache
import com.apollographql.apollo.cache.normalized.internal.Platform
import okio.internal.commonAsUtf8ToByteArray
import kotlin.reflect.KClass

/**
 * Memory (multiplatform) cache implementation based on recently used property (LRU).
 *
 * [maxSizeBytes] - the maximum size of bytes the cache may occupy.
 * [expireAfterMillis] - after what timeout each entry in the cache treated as expired. By default there is no timeout.
 *
 * Expired entries removed from the cache only on cache miss ([loadRecord] operation) and not removed from the cache automatically
 * (there is no any sort of GC that runs in the background).
 */
class MemoryCache(
    private val maxSizeBytes: Int,
    private val expireAfterMillis: Long = -1,
) : NormalizedCache() {
  private val lruCache = LruCache<String, CacheEntry>(maxSize = maxSizeBytes) { key, cacheEntry ->
    key.commonAsUtf8ToByteArray().size + (cacheEntry?.sizeInBytes ?: 0)
  }

  val size: Int
    get() = lruCache.size()

  override fun loadRecord(key: String, cacheHeaders: CacheHeaders): Record? {
    val cachedEntry = lruCache[key]
    return if (cachedEntry == null || cachedEntry.isExpired) {
      if (cachedEntry != null) {
        lruCache.remove(key)
      }
      nextCache?.loadRecord(key, cacheHeaders)
    } else {
      if (cacheHeaders.hasHeader(ApolloCacheHeaders.EVICT_AFTER_READ)) {
        lruCache.remove(key)
      }
      cachedEntry.record
    }
  }

  override fun clearAll() {
    lruCache.clear()
    nextCache?.clearAll()
  }

  override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
    val cachedEntry = lruCache.remove(cacheKey.key)
    if (cascade && cachedEntry != null) {
      for (cacheReference in cachedEntry.record.referencedFields()) {
        remove(CacheKey(cacheReference.key), true)
      }
    }

    val removeFromNextCacheResult = nextCache?.remove(cacheKey, cascade) ?: false

    return cachedEntry != null || removeFromNextCacheResult
  }

  override fun performMerge(apolloRecord: Record, oldRecord: Record?, cacheHeaders: CacheHeaders): Set<String> {
    return if (oldRecord == null) {
      lruCache[apolloRecord.key] = CacheEntry(
          record = apolloRecord,
          expireAfterMillis = expireAfterMillis
      )
      apolloRecord.keys()
    } else {
      oldRecord.mergeWith(apolloRecord).also {
        //re-insert to trigger new weight calculation
        lruCache[apolloRecord.key] = CacheEntry(
            record = oldRecord,
            expireAfterMillis = expireAfterMillis
        )
      }
    }
  }

  @OptIn(ExperimentalStdlibApi::class)
  override fun dump() = buildMap<KClass<*>, Map<String, Record>> {
    put(this@MemoryCache::class, lruCache.dump().mapValues { (_, entry) -> entry.record })
    putAll(nextCache?.dump().orEmpty())
  }

  internal fun clearCurrentCache() {
    lruCache.clear()
  }

  private class CacheEntry(
      val record: Record,
      val expireAfterMillis: Long
  ) {
    val cachedAtMillis: Long = Platform.currentTimeMillis()

    val sizeInBytes: Int = record.sizeEstimateBytes() + 8

    val isExpired: Boolean
      get() {
        return if (expireAfterMillis < 0) {
          false
        } else {
          Platform.currentTimeMillis() - cachedAtMillis >= expireAfterMillis
        }
      }
  }
}
