@file:Suppress("DEPRECATION")

package com.apollographql.apollo.cache.normalized.api

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KClass

@Deprecated("Use the new Normalized Cache at https://github.com/apollographql/apollo-kotlin-normalized-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
interface ReadOnlyNormalizedCache {
  /**
   * @param key          The key of the record to read.
   * @param cacheHeaders The cache headers associated with the request which generated this record.
   * @return The [Record] for key. If not present return null.
   */
  fun loadRecord(key: String, cacheHeaders: CacheHeaders): Record?

  /**
   * Calls through to [NormalizedCache.loadRecord]. Implementations should override this
   * method if the underlying storage technology can offer an optimized manner to read multiple records.
   * There is no guarantee on the order of returned [Record]
   *
   * @param keys         The set of [Record] keys to read.
   * @param cacheHeaders The cache headers associated with the request which generated this record.
   */
  fun loadRecords(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record>

  fun dump(): Map<@JvmSuppressWildcards KClass<*>, Map<String, Record>>
}
