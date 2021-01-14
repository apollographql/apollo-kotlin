package com.apollographql.apollo.cache.normalized

import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.cache.ApolloCacheHeaders
import com.apollographql.apollo.cache.CacheHeaders
import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KClass

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
 * See [com.apollographql.apollo.cache.normalized.lru.LruNormalizedCache] for a in memory cache.
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

  abstract fun stream(key: String, cacheHeaders: CacheHeaders): JsonReader?

  /**
   * Calls through to [NormalizedCache.loadRecord]. Implementations should override this
   * method if the underlying storage technology can offer an optimized manner to read multiple records.
   *
   * @param keys         The set of [Record] keys to read.
   * @param cacheHeaders The cache headers associated with the request which generated this record.
   */
  open fun loadRecords(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
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
  open fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
    if (cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE)) {
      return emptySet<String>()
    }
    val nextCacheChangedKeys = nextCache?.merge(record, cacheHeaders).orEmpty()
    val currentCacheChangedKeys = performMerge(record, loadRecord(record.key, cacheHeaders), cacheHeaders)
    val changedKeys: MutableSet<String> = HashSet()
    changedKeys.addAll(nextCacheChangedKeys)
    changedKeys.addAll(currentCacheChangedKeys)
    return changedKeys
  }

  /**
   * Calls through to [NormalizedCache.merge]. Implementations should override this method
   * if the underlying storage technology can offer an optimized manner to store multiple records.
   *
   * @param recordCollection The collection of Records to merge.
   * @param cacheHeaders The [CacheHeaders] associated with the request which generated this record.
   * @return A set of record field keys that have changed. This set is returned by [Record.mergeWith].
   */
  open fun merge(recordCollection: Collection<Record>, cacheHeaders: CacheHeaders): Set<String> {
    if (cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE)) {
      return emptySet()
    }
    val nextCacheChangedKeys = nextCache?.merge(recordCollection, cacheHeaders).orEmpty()
    val currentCacheChangedKeys: MutableSet<String> = HashSet()
    val oldRecords = loadRecords(recordCollection.map { it.key }, cacheHeaders).associateBy { it.key }
    for (record in recordCollection) {
      val oldRecord = oldRecords[record.key]
      currentCacheChangedKeys.addAll(performMerge(record, oldRecord, cacheHeaders))
    }
    val changedKeys: MutableSet<String> = HashSet()
    changedKeys.addAll(nextCacheChangedKeys)
    changedKeys.addAll(currentCacheChangedKeys)
    return changedKeys
  }

  /**
   * @param apolloRecord The new record to merge into [existingRecord]
   * @param oldRecord The current record before merge. Null if no record exists.
   *
   * @return A set of record field keys that have changed. This set should be computed by [Record.mergeWith].
   */
  protected abstract fun performMerge(apolloRecord: Record, oldRecord: Record?, cacheHeaders: CacheHeaders): Set<String>

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

  open fun dump(): Map<@JvmSuppressWildcards KClass<*>, Map<String, Record>> {
    return mapOf(this::class to emptyMap())
  }

  companion object {

    @JvmStatic
    fun prettifyDump(dump: Map<@JvmSuppressWildcards KClass<*>, Map<String, Record>>) = buildString {
      for ((key, value) in dump) {
        append(key.simpleName)
            .append(" {")
        for ((key1, value1) in value) {
          append("\n  \"")
              .append(key1)
              .append("\" : {")
          for ((key2, value2) in value1.fields) {
            append("\n    \"")
                .append(key2)
                .append("\" : ")
            when (value2) {
              is CacheReference -> {
                append("CacheRecordRef(")
                    .append(value2)
                    .append(")")
              }
              is List<*> -> {
                append("[")
                for (item in value2) {
                  append("\n      ")
                      .append(if (item is CacheReference) "CacheRecordRef(" else "")
                      .append(item)
                      .append(if (item is CacheReference) ")" else "")
                }
                append("\n    ]")
              }
              else -> append(value2)
            }
          }
          append("\n  }\n")
        }
        append("}\n")
      }
    }
  }
}
