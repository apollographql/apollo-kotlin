package com.apollographql.apollo.cache.normalized

import com.apollographql.apollo.api.internal.Optional
import com.apollographql.apollo.cache.ApolloCacheHeaders
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCache
import java.util.ArrayList
import java.util.HashSet

/**
 * A provider of [Record] for reading requests from cache.
 *
 * To serialize a [Record] to a standardized form use recordAdapter() which handles call custom scalar
 * types registered on the ApolloClient
 *
 * If a [NormalizedCache] cannot return all the records needed to read a response, it will be considered a cache
 * miss.
 *
 * A [NormalizedCache] is recommended to implement support for [CacheHeaders] specified in [ ].
 *
 * A [NormalizedCache] can choose to store records in any manner.
 *
 * See [LruNormalizedCache] for a in memory cache.
 */
abstract class NormalizedCache {
  var nextCache: NormalizedCache? = null
    private set

  /**
   * @param key          The key of the record to read.
   * @param cacheHeaders The cache headers associated with the request which generated this record.
   * @return The [Record] for key. If not present return null.
   */
  abstract fun loadRecord(key: String, cacheHeaders: CacheHeaders): Record?

  /**
   * Calls through to [NormalizedCache.loadRecord]. Implementations should override this
   * method if the underlying storage technology can offer an optimized manner to read multiple records.
   *
   * @param keys         The set of [Record] keys to read.
   * @param cacheHeaders The cache headers associated with the request which generated this record.
   */
  fun loadRecords(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
    val records: MutableList<Record> = ArrayList(keys.size)
    for (key in keys) {
      val record = loadRecord(key, cacheHeaders)
      if (record != null) {
        records.add(record)
      }
    }
    return records
  }

  /**
   * @param record       The [Record] to merge.
   * @param cacheHeaders The [CacheHeaders] associated with the request which generated this record.
   * @return A set of record field keys that have changed. This set is returned by [Record.mergeWith].
   */
  open fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String?> {
    if (cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE)) {
      return emptySet<String>()
    }
    val nextCacheChangedKeys = nextCache?.merge(record, cacheHeaders).orEmpty()
    val currentCacheChangedKeys = performMerge(record, cacheHeaders)
    val changedKeys: MutableSet<String?> = HashSet()
    changedKeys.addAll(nextCacheChangedKeys)
    changedKeys.addAll(currentCacheChangedKeys)
    return changedKeys
  }

  /**
   * Calls through to [NormalizedCache.merge]. Implementations should override this method
   * if the underlying storage technology can offer an optimized manner to store multiple records.
   *
   * @param recordSet    The set of Records to merge.
   * @param cacheHeaders The [CacheHeaders] associated with the request which generated this record.
   * @return A set of record field keys that have changed. This set is returned by [Record.mergeWith].
   */
  open fun merge(recordSet: Collection<Record>, cacheHeaders: CacheHeaders): Set<String> {
    if (cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE)) {
      return emptySet()
    }
    val nextCacheChangedKeys = nextCache?.merge(recordSet, cacheHeaders).orEmpty()
    val currentCacheChangedKeys: MutableSet<String> = HashSet()
    for (record in recordSet) {
      currentCacheChangedKeys.addAll(performMerge(record, cacheHeaders))
    }
    val changedKeys: MutableSet<String> = HashSet()
    changedKeys.addAll(nextCacheChangedKeys)
    changedKeys.addAll(currentCacheChangedKeys)
    return changedKeys
  }

  protected abstract fun performMerge(apolloRecord: Record, cacheHeaders: CacheHeaders): Set<String>

  /**
   * Clears all records from the cache.
   *
   * Clients should call ApolloClient#clearNormalizedCache() for a thread-safe access to this method.
   */
  abstract fun clearAll()

  /**
   * Remove cached record by the key
   *
   * @param cacheKey of record to be removed
   * @return `true` if record with such key was successfully removed, `false` otherwise
   */
  fun remove(cacheKey: CacheKey): Boolean {
    return remove(cacheKey, false)
  }

  /**
   * Remove cached record by the key
   *
   * @param cacheKey of record to be removed
   * @param cascade defines if remove operation is propagated to the referenced entities
   * @return `true` if record with such key was successfully removed, `false` otherwise
   */
  abstract fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean

  fun chain(cache: NormalizedCache) = apply {
    var leafCache = this
    while (leafCache.nextCache != null) {
      leafCache = leafCache.nextCache!!
    }
    leafCache.nextCache = cache
  }

  @Deprecated("Use property instead", replaceWith = ReplaceWith("nextCache"))
  fun nextCache(): Optional<NormalizedCache> = Optional.fromNullable(nextCache)

  open fun dump(): Map<@JvmSuppressWildcards Class<*>, Map<String, Record>> {
    val clazz: Class<*> = this.javaClass
    return mapOf(clazz to emptyMap())
  }
}
