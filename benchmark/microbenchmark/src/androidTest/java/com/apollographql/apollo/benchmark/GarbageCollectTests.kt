package com.apollographql.apollo.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.benchmark.Utils.dbFile
import com.apollographql.apollo.benchmark.Utils.getDbName
import com.apollographql.apollo.conferences.cache.Cache
import com.apollographql.apollo.conferences.cache.Cache.cache
import com.apollographql.cache.normalized.CacheManager
import com.apollographql.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.cache.normalized.api.SchemaCoordinatesMaxAgeProvider
import com.apollographql.cache.normalized.apolloStore
import com.apollographql.cache.normalized.garbageCollect
import com.apollographql.cache.normalized.memory.MemoryCacheFactory
import com.apollographql.cache.normalized.sql.SqlNormalizedCacheFactory
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.days

class GarbageCollectTests {
  @get:Rule
  val benchmarkRule = BenchmarkRule()

  @Test
  fun garbageCollectMemory() {
    lateinit var cacheManager: CacheManager
    benchmarkRule.measureRepeated {
      runWithTimingDisabled {
        cacheManager = cacheManager(MemoryCacheFactory())
        primeCache(cacheManager)
      }
      runBlocking {
        cacheManager.accessCache {
          it.garbageCollect(maxAgeProvider)
        }
      }
    }
  }

  @Test
  fun garbageCollectSql() {
    lateinit var cacheManager: CacheManager
    benchmarkRule.measureRepeated {
      runWithTimingDisabled {
        dbFile.delete()
        cacheManager = cacheManager(SqlNormalizedCacheFactory(getDbName()))
        primeCache(cacheManager)
      }
      runBlocking {
        cacheManager.accessCache {
          it.garbageCollect(maxAgeProvider)
        }
      }
    }
  }

  @Test
  fun garbageCollectMemoryThenSql() {
    lateinit var cacheManager: CacheManager
    benchmarkRule.measureRepeated {
      runWithTimingDisabled {
        dbFile.delete()
        cacheManager = cacheManager(MemoryCacheFactory().chain(SqlNormalizedCacheFactory(getDbName())))
        primeCache(cacheManager)
      }
      runBlocking {
        cacheManager.accessCache {
          it.garbageCollect(maxAgeProvider)
        }
      }
    }
  }
}

private fun cacheManager(factory: NormalizedCacheFactory): CacheManager =
  ApolloClient.Builder().cache(factory).serverUrl("http://unused").build().apolloStore.cacheManager

private val maxAgeProvider = SchemaCoordinatesMaxAgeProvider(
    Cache.maxAges,
    defaultMaxAge = 1.days,
)
