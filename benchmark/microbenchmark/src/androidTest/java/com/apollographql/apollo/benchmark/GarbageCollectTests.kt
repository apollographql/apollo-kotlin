package com.apollographql.apollo.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import com.apollographql.apollo.benchmark.Utils.dbFile
import com.apollographql.apollo.benchmark.Utils.dbName
import com.apollographql.apollo.conferences.cache.Cache
import com.apollographql.cache.normalized.ApolloStore
import com.apollographql.cache.normalized.api.SchemaCoordinatesMaxAgeProvider
import com.apollographql.cache.normalized.garbageCollect
import com.apollographql.cache.normalized.memory.MemoryCacheFactory
import com.apollographql.cache.normalized.sql.SqlNormalizedCacheFactory
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.days

class GarbageCollectTests {
  @get:Rule
  val benchmarkRule = BenchmarkRule()

  @Test
  fun garbageCollectMemory() {
    lateinit var store: ApolloStore
    benchmarkRule.measureRepeated {
      runWithTimingDisabled {
        store = ApolloStore(MemoryCacheFactory())
        primeCache(store)
      }
      store.garbageCollect(maxAgeProvider)
    }
  }

  @Test
  fun garbageCollectSql() {
    lateinit var store: ApolloStore
    benchmarkRule.measureRepeated {
      runWithTimingDisabled {
        dbFile.delete()
        store = ApolloStore(SqlNormalizedCacheFactory(dbName))
        primeCache(store)
      }
      store.garbageCollect(maxAgeProvider)
    }
  }

  @Test
  fun garbageCollectMemoryThenSql() {
    lateinit var store: ApolloStore
    benchmarkRule.measureRepeated {
      runWithTimingDisabled {
        dbFile.delete()
        store = ApolloStore(MemoryCacheFactory().chain(SqlNormalizedCacheFactory(dbName)))
        primeCache(store)
      }
      store.garbageCollect(maxAgeProvider)
    }
  }
}

private val maxAgeProvider = SchemaCoordinatesMaxAgeProvider(
    Cache.maxAges,
    defaultMaxAge = 1.days,
)
