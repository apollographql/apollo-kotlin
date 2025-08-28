package com.apollographql.apollo.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import com.apollographql.apollo.benchmark.Utils.dbFile
import com.apollographql.apollo.benchmark.Utils.dbName
import com.apollographql.apollo.conferences.cache.Cache
import com.apollographql.cache.normalized.ApolloStore
import com.apollographql.cache.normalized.CacheManager
import com.apollographql.cache.normalized.api.SchemaCoordinatesMaxAgeProvider
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
        cacheManager = CacheManager(MemoryCacheFactory())
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
        cacheManager = CacheManager(SqlNormalizedCacheFactory(dbName))
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
        cacheManager = CacheManager(MemoryCacheFactory().chain(SqlNormalizedCacheFactory(dbName)))
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

private val maxAgeProvider = SchemaCoordinatesMaxAgeProvider(
    Cache.maxAges,
    defaultMaxAge = 1.days,
)
