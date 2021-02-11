package com.apollographql.apollo.benchmark

import Utils.bufferedSource
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.platform.app.InstrumentationRegistry
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.api.parse
import com.apollographql.apollo.benchmark.moshi.Query
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.MemoryCache
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.internal.ReadMode
import com.apollographql.apollo.cache.normalized.internal.ReadableStore
import com.apollographql.apollo.cache.normalized.internal.normalize
import com.apollographql.apollo.cache.normalized.internal.readDataFromCache
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCache
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory
import com.squareup.moshi.Moshi
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class Benchmark {
  @get:Rule
  val benchmarkRule = BenchmarkRule()

  private val moshiAdapter = Moshi.Builder().build().adapter(Query::class.java)

  @Test
  fun moshi() = benchmarkRule.measureRepeated {
    val bufferedSource = runWithTimingDisabled {
      bufferedSource()
    }

    moshiAdapter.fromJson(bufferedSource)
  }

  @Test
  fun apollo() = benchmarkRule.measureRepeated {
    val bufferedSource = runWithTimingDisabled {
      bufferedSource()
    }

    operation.parse(bufferedSource)
  }

  @Test
  fun apolloParseAndNormalize() = benchmarkRule.measureRepeated {
    val bufferedSource = runWithTimingDisabled {
      bufferedSource()
    }

    val data = operation.parse(bufferedSource).data!!
    val records = operation.normalize(data, CustomScalarAdapters.DEFAULT, CacheKeyResolver.DEFAULT)
  }

  companion object {
    lateinit var sqlReadableStore: ReadableStore
    lateinit var memoryReadableStore: ReadableStore
    private val operation = GetResponseQuery()

    @BeforeClass
    @JvmStatic
    fun setup() {
      val data = operation.parse(bufferedSource()).data!!
      val records = operation.normalize(data, CustomScalarAdapters.DEFAULT, CacheKeyResolver.DEFAULT)

      val sqlCache = SqlNormalizedCacheFactory(context = InstrumentationRegistry.getInstrumentation().context).create()
      sqlCache.merge(records, CacheHeaders.NONE)
      sqlReadableStore = object : ReadableStore {
        override fun read(key: String, cacheHeaders: CacheHeaders): Record? {
          return sqlCache.loadRecord(key, cacheHeaders)
        }

        override fun read(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
          return sqlCache.loadRecords(keys, cacheHeaders)
        }
      }

      val memoryCache = MemoryCache(Int.MAX_VALUE)
      memoryCache.merge(records, CacheHeaders.NONE)
      memoryReadableStore = object : ReadableStore {
        override fun read(key: String, cacheHeaders: CacheHeaders): Record? {
          return memoryCache.loadRecord(key, cacheHeaders)
        }

        override fun read(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
          return memoryCache.loadRecords(keys, cacheHeaders)
        }
      }
    }
  }


  @Test
  fun apolloReadCacheSql() = benchmarkRule.measureRepeated {
    val data2 = operation.readDataFromCache(CustomScalarAdapters.DEFAULT, sqlReadableStore, CacheKeyResolver.DEFAULT, CacheHeaders.NONE)
  }

  @Test
  fun apolloBatchCacheSql() = benchmarkRule.measureRepeated {
    val data2 = operation.readDataFromCache(CustomScalarAdapters.DEFAULT, sqlReadableStore, CacheKeyResolver.DEFAULT, CacheHeaders.NONE, ReadMode.BATCH)
  }

  @Test
  fun apolloReadCacheMemory() = benchmarkRule.measureRepeated {
    val data2 = operation.readDataFromCache(CustomScalarAdapters.DEFAULT, memoryReadableStore, CacheKeyResolver.DEFAULT, CacheHeaders.NONE)
  }

  @Test
  fun apolloBatchCacheMemory() = benchmarkRule.measureRepeated {
    val data2 = operation.readDataFromCache(CustomScalarAdapters.DEFAULT, memoryReadableStore, CacheKeyResolver.DEFAULT, CacheHeaders.NONE, ReadMode.BATCH)
  }
}