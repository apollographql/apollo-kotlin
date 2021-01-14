package com.apollographql.apollo.internal

import com.apollographql.apollo.NamedCountDownLatch
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.internal.ApolloLogger
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.NormalizedCache
import com.apollographql.apollo.cache.normalized.Record
import org.junit.Test
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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

          override fun stream(key: String, cacheHeaders: CacheHeaders): JsonReader? {
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

          override fun performMerge(apolloRecord: Record, oldRecord: Record?, cacheHeaders: CacheHeaders): Set<String> {
            return emptySet()
          }
        },
        CacheKeyResolver.DEFAULT,
        CustomScalarAdapters(emptyMap()),
        Executors.newSingleThreadExecutor(),
        ApolloLogger(null)
    )
    realApolloStore.clearAll().execute()
    latch.awaitOrThrowWithTimeout(3, TimeUnit.SECONDS)
  }
}