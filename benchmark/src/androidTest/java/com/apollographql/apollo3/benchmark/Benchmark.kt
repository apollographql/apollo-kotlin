package com.apollographql.apollo3.benchmark

import Utils.bufferedSource
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.platform.app.InstrumentationRegistry
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.fromResponse
import com.apollographql.apollo3.benchmark.moshi.Query
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.api.CacheKeyResolver
import com.apollographql.apollo3.cache.normalized.MemoryCache
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.cache.normalized.internal.ApolloStore
import com.apollographql.apollo3.cache.normalized.internal.normalize
import com.apollographql.apollo3.cache.normalized.internal.readDataFromCache
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class Benchmark {
  @get:Rule
  val benchmarkRule = BenchmarkRule()


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

    operation.fromResponse(bufferedSource, customScalarAdapters)
  }

  @Test
  fun apolloParseAndNormalize() = benchmarkRule.measureRepeated {
    val bufferedSource = runWithTimingDisabled {
      bufferedSource()
    }

    val data = operation.fromResponse(bufferedSource, customScalarAdapters).data!!
    val records = operation.normalize(data, ResponseAdapterCache.DEFAULT)
  }

  @Test
  fun apolloBatchCacheSql() = benchmarkRule.measureRepeated {
    runBlocking {
      sqlStore.readOperation(
          operation = operation,
          responseAdapterCache = ResponseAdapterCache.DEFAULT,
          cacheHeaders = CacheHeaders.NONE,
      )
    }
  }

  @Test
  fun apolloBatchCacheMemory() = benchmarkRule.measureRepeated {
    runBlocking {
      memoryStore.readOperation(
          operation = operation,
          responseAdapterCache = ResponseAdapterCache.DEFAULT,
          cacheHeaders = CacheHeaders.NONE,
      )
    }
  }

  companion object {
    lateinit var sqlStore: ApolloStore
    lateinit var memoryStore: ApolloStore
    private val operation = GetResponseQuery()
    private val moshiAdapter = Moshi.Builder().build().adapter(Query::class.java)
    private val customScalarAdapters = ResponseAdapterCache(emptyMap())

    @BeforeClass
    @JvmStatic
    fun setup() {
      val data = operation.fromResponse(bufferedSource()).data!!
      val records = operation.normalize(data, ResponseAdapterCache.DEFAULT, CacheKeyResolver.DEFAULT).values

      sqlStore = ApolloStore(SqlNormalizedCacheFactory(context = InstrumentationRegistry.getInstrumentation().context), CacheKeyResolver.DEFAULT)
      runBlocking {
        sqlStore.writeOperation(operation, data)
      }

      memoryStore = ApolloStore(MemoryCacheFactory(), CacheKeyResolver.DEFAULT)
      runBlocking {
        memoryStore.writeOperation(operation, data)
      }
    }
  }
}