package com.apollographql.apollo.cache.normalized.api

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
 */
abstract class NormalizedCache : ReadOnlyNormalizedCache {
  var nextCache: NormalizedCache? = null
    private set

  /**
   * @param record       The [Record] to merge.
   * @param cacheHeaders The [CacheHeaders] associated with the request which generated this record.
   * @return A set of record field keys that have changed. This set is returned by [Record.mergeWith].
   */
  abstract fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String>

  /**
   * Calls through to [NormalizedCache.merge]. Implementations should override this method
   * if the underlying storage technology can offer an optimized manner to store multiple records.
   *
   * @param records The collection of Records to merge.
   * @param cacheHeaders The [CacheHeaders] associated with the request which generated this record.
   * @return A set of record field keys that have changed. This set is returned by [Record.mergeWith].
   */
  abstract fun merge(records: Collection<Record>, cacheHeaders: CacheHeaders): Set<String>

  /**
   * Clears all records from the cache.
   */
  abstract fun clearAll()

  /**
   * Remove a record and potentially its referenced records from this cache and all chained caches
   *
   * @param cacheKey of record to be removed
   * @param cascade remove referenced records if true
   * @return `true` if a record with such key was successfully removed, `false` otherwise
   */
  abstract fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean

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
  abstract fun remove(pattern: String): Int

  fun chain(cache: NormalizedCache) = apply {
    var leafCache = this
    while (leafCache.nextCache != null) {
      leafCache = leafCache.nextCache!!
    }
    leafCache.nextCache = cache
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
              is CacheKey -> {
                append(value2)
              }
              is List<*> -> {
                append("[")
                for (item in value2) {
                  append("\n      ")
                      .append(item)
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

    /**
     * A tentative to approximate the Sqlite LIKE operator with Regexes
     */
    fun patternToRegex(pattern: String): Regex {
      val regex = buildString {
        var pendingEscape = false
        for (i in pattern.indices) {
          val cur = pattern[i]
          when {
            pendingEscape -> {
              when {
                cur == '\\' -> append("\\\\") // an escaped backslash is also an escape backslash in a regex
                cur == '%' -> append("%")
                cur == '_' -> append("_")
                else -> error("Invalid escape in pattern: $pattern")
              }
            }
            cur == '\\' -> pendingEscape = true
            cur == '%' -> append(".*")
            cur == '_' -> append(".")
            else -> {
              if (specialChars.contains(cur)) {
                // this needs to be escaped in the regex
                append("\\")
              }
              append(cur)
            }
          }
        }
      }

      return Regex(regex, option = RegexOption.IGNORE_CASE)
    }

    private val specialChars = "()^$.*?+{}"
  }
}

