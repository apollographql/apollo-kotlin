package com.apollographql.apollo3.benchmark

import Utils.resource
import android.app.Instrumentation
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.platform.app.InstrumentationRegistry
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import com.apollographql.apollo3.benchmark.moshi.Query
import com.apollographql.apollo3.benchmark.test.R
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.normalize
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
      resource(R.raw.largesample)
    }

    moshiAdapter.fromJson(bufferedSource)
  }

  @Test
  fun apollo() = benchmarkRule.measureRepeated {
    val bufferedSource = runWithTimingDisabled {
      resource(R.raw.largesample)
    }

    operation.parseJsonResponse(bufferedSource.jsonReader(), customScalarAdapters)
  }

  @Test
  fun apolloParseAndNormalize() = benchmarkRule.measureRepeated {
    val bufferedSource = runWithTimingDisabled {
      resource(R.raw.largesample)
    }

    val data = operation.parseJsonResponse(bufferedSource.jsonReader(), customScalarAdapters).data!!
    val records = operation.normalize(data, customScalarAdapters, TypePolicyCacheKeyGenerator)
  }

  @Test
  fun apolloBatchCacheSql() = benchmarkRule.measureRepeated {
    runBlocking {
      sqlStore.readOperation(
          operation = operation,
          customScalarAdapters = customScalarAdapters,
          cacheHeaders = CacheHeaders.NONE,
      )
    }
  }

  @Test
  fun apolloBatchCacheMemory() = benchmarkRule.measureRepeated {
    runBlocking {
      memoryStore.readOperation(
          operation = operation,
          customScalarAdapters = customScalarAdapters,
          cacheHeaders = CacheHeaders.NONE,
      )
    }
  }

  companion object {
    lateinit var sqlStore: ApolloStore
    lateinit var memoryStore: ApolloStore
    private val operation = GetResponseQuery()
    private val moshiAdapter = Moshi.Builder().build().adapter(Query::class.java)
    private val customScalarAdapters = CustomScalarAdapters.Empty

    @BeforeClass
    @JvmStatic
    fun setup() {
      val data = operation.parseJsonResponse(resource(R.raw.largesample).jsonReader()).data!!
      val records = operation.normalize(data, customScalarAdapters, TypePolicyCacheKeyGenerator).values

      sqlStore = ApolloStore(SqlNormalizedCacheFactory())
      runBlocking {
        sqlStore.writeOperation(operation, data)
      }

      memoryStore = ApolloStore(MemoryCacheFactory())
      runBlocking {
        memoryStore.writeOperation(operation, data)
      }
    }
  }
}