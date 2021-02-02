package com.apollographql.apollo.internal

import com.apollographql.apollo.NamedCountDownLatch
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.internal.ApolloLogger
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.internal.RealApolloStore
import com.apollographql.apollo.cache.normalized.NormalizedCache
import com.apollographql.apollo.cache.normalized.Record
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class ApolloStoreTest {
  @Test
  @Throws(Exception::class)
  fun storeClearAllCallsNormalizedCacheClearAll() {
    val latch = NamedCountDownLatch("storeClearAllCallsNormalizedCacheClearAll", 1)
    val realApolloStore = RealApolloStore(
        object : NormalizedCache() {
          override fun loadRecord(key: String, cacheHeaders: CacheHeaders): Record? {
            return null
          }

          override fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
            return emptySet<String>()
          }

          override fun clearAll() {
            latch.countDown()
          }

          override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
            return false
          }

          override fun loadRecords(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
            return emptyList()
          }

          override fun merge(records: Collection<Record>, cacheHeaders: CacheHeaders): Set<String> {
            return emptySet()
          }

          override fun dump(): Map<@JvmSuppressWildcards KClass<*>, Map<String, Record>> {
            return emptyMap()
          }
        },
        CacheKeyResolver.DEFAULT,
        CustomScalarAdapters(emptyMap()),
        ApolloLogger(null)
    )
    realApolloStore.clearAll()
    latch.awaitOrThrowWithTimeout(3, TimeUnit.SECONDS)
  }
}
