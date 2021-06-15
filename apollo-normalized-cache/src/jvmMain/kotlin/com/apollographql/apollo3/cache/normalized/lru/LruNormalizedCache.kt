package com.apollographql.apollo3.cache.normalized.lru

import com.apollographql.apollo3.cache.ApolloCacheHeaders
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.NormalizedCache
import com.apollographql.apollo3.cache.normalized.Record
import com.nytimes.android.external.cache.Cache
import com.nytimes.android.external.cache.CacheBuilder
import java.nio.charset.Charset
import kotlin.reflect.KClass

@Deprecated("Will be removed soon", replaceWith = ReplaceWith("MemoryCache", "com.apollographql.apollo3.cache.normalized.MemoryCache"))
class LruNormalizedCache internal constructor(evictionPolicy: EvictionPolicy) : NormalizedCache() {

  private val lruCache: Cache<String, Record> = CacheBuilder.newBuilder().apply {
    if (evictionPolicy.maxSizeBytes != null) {
      maximumWeight(evictionPolicy.maxSizeBytes).weigher { key: String, value: Record ->
        key.toByteArray(Charset.defaultCharset()).size + value.sizeInBytes
      }
    }
    if (evictionPolicy.maxEntries != null) {
      maximumSize(evictionPolicy.maxEntries)
    }
    if (evictionPolicy.expireAfterAccess != null) {
      expireAfterAccess(evictionPolicy.expireAfterAccess, evictionPolicy.expireAfterAccessTimeUnit!!)
    }
    if (evictionPolicy.expireAfterWrite != null) {
      expireAfterWrite(evictionPolicy.expireAfterWrite, evictionPolicy.expireAfterWriteTimeUnit!!)
    }
  }.build()

  override fun loadRecord(key: String, cacheHeaders: CacheHeaders): Record? {
    return try {
      lruCache.get(key) {
        nextCache?.loadRecord(key, cacheHeaders)
      }
    } catch (ignored: Exception) { // Thrown when the nextCache's value is null
      return null
    }.also {
      if (cacheHeaders.hasHeader(ApolloCacheHeaders.EVICT_AFTER_READ)) {
        lruCache.invalidate(key)
      }
    }
  }

  override fun loadRecords(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
    return keys.mapNotNull { key -> loadRecord(key, cacheHeaders) }
  }

  override fun clearAll() {
    nextCache?.clearAll()
    clearCurrentCache()
  }

  override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
    var result: Boolean = nextCache?.remove(cacheKey, cascade) ?: false

    val record = lruCache.getIfPresent(cacheKey.key)
    if (record != null) {
      lruCache.invalidate(cacheKey.key)
      result = true
      if (cascade) {
        for (cacheReference in record.referencedFields()) {
          result = result && remove(CacheKey(cacheReference.key), true)
        }
      }
    }
    return result
  }

  override fun remove(pattern: String): Int {
    TODO("Not yet implemented")
  }

  override fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
    if (cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE)) {
      return emptySet()
    }

    val oldRecord = loadRecord(record.key, cacheHeaders)
    val changedKeys = if (oldRecord == null) {
      lruCache.put(record.key, record)
      record.fieldKeys()
    } else {
      val (mergedRecord, changedKeys) = oldRecord.mergeWith(record)
      lruCache.put(record.key, mergedRecord)
      changedKeys
    }

    return changedKeys + nextCache?.merge(record, cacheHeaders).orEmpty()
  }

  override fun merge(records: Collection<Record>, cacheHeaders: CacheHeaders): Set<String> {
    if (cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE)) {
      return emptySet()
    }
    return records.flatMap { record -> merge(record, cacheHeaders) }.toSet()
  }

  private fun clearCurrentCache() {
    lruCache.invalidateAll()
  }

  @OptIn(ExperimentalStdlibApi::class)
  override fun dump() = buildMap<KClass<*>, Map<String, Record>> {
    put(this@LruNormalizedCache::class, lruCache.asMap())
    putAll(nextCache?.dump().orEmpty())
  }
}
