package com.apollographql.apollo3.cache.normalized.api

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
 * A [NormalizedCache] is recommended to implement support for [CacheHeaders] specified in the `cacheHeaders` of [merge] .
 *
 * A [NormalizedCache] can choose to store records in any manner.
 */
interface NormalizedCache : ReadOnlyNormalizedCache {
  /**
   * @param record       The [Record] to merge.
   * @param cacheHeaders The [CacheHeaders] associated with the request which generated this record.
   * @param recordMerger The [RecordMerger] to use when merging the record.
   * @return A set of record field keys that have changed. This set is returned by [RecordMerger.merge].
   */
  fun merge(
      record: Record,
      cacheHeaders: CacheHeaders,
      recordMerger: RecordMerger,
  ): Set<String>

  /**
   * Calls through to [NormalizedCache.merge]. Implementations should override this method
   * if the underlying storage technology can offer an optimized manner to store multiple records.
   *
   * @param records The collection of Records to merge.
   * @param cacheHeaders The [CacheHeaders] associated with the request which generated this record.
   * @param recordMerger The [RecordMerger] to use when merging the records.
   * @return A set of record field keys that have changed. This set is returned by [RecordMerger.merge].
   */
  fun merge(
      records: Collection<Record>,
      cacheHeaders: CacheHeaders,
      recordMerger: RecordMerger,
  ): Set<String>


  /**
   * Clears all records from the cache.
   */
  fun clearAll()

  /**
   * Remove a record and potentially its referenced records from this cache and all chained caches
   *
   * @param cacheKey of record to be removed
   * @param cascade remove referenced records if true
   * @return `true` if a record with such key was successfully removed, `false` otherwise
   */
  fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean

  /**
   * Remove records whose key matches a given pattern from this cache and all chained caches
   *
   * @param pattern a pattern to filter the cache keys. 'pattern' is interpreted as in the LIKE operator of Sqlite.
   * - '%' matches any sequence of zero or more characters
   * - '_' matches any single character
   * - The matching is case-insensitive
   * - '\' is used as escape
   * See https://sqlite.org/lang_expr.html for more details
   *
   * @return the number of records deleted accross all caches
   */
  fun remove(pattern: String): Int


  companion object {
    @JvmStatic
    fun prettifyDump(dump: Map<@JvmSuppressWildcards KClass<*>, Map<String, Record>>): String = dump.prettifyDump()

    private fun Any?.prettifyDump(level: Int = 0): String {
      return buildString {
        when (this@prettifyDump) {
          is Record -> {
            append("{\n")
            indent(level + 1)
            append("fields: ")
            append(fields.prettifyDump(level + 1))
            append("\n")
            indent(level + 1)
            append("metadata: ")
            append(metadata.prettifyDump(level + 1))
            append("\n")
            indent(level + 1)
            append("dates: ")
            append(dates.prettifyDump(level + 1))
            append("\n")
            indent(level)
            append("}")
          }

          is List<*> -> {
            append("[")
            if (this@prettifyDump.isNotEmpty()) {
              append("\n")
              for (value in this@prettifyDump) {
                indent(level + 1)
                append(value.prettifyDump(level + 1))
                append("\n")
              }
              indent(level)
            }
            append("]")
          }

          is Map<*, *> -> {
            append("{")
            if (this@prettifyDump.isNotEmpty()) {
              append("\n")
              for ((key, value) in this@prettifyDump) {
                indent(level + 1)
                append(when (key) {
                  is KClass<*> -> key.simpleName
                  else -> key
                })
                append(": ")
                append(value.prettifyDump(level + 1))
                append("\n")
              }
              indent(level)
            }
            append("}")
          }

          else -> append(this@prettifyDump)
        }
      }
    }

    private fun StringBuilder.indent(level: Int) = append("  ".repeat(level))
  }
}

