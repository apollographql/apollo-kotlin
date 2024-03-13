package com.apollographql.apollo3.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.platform.app.InstrumentationRegistry
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import com.apollographql.apollo3.benchmark.Utils.checkOperationBased
import com.apollographql.apollo3.benchmark.Utils.checkResponseBased
import com.apollographql.apollo3.benchmark.Utils.operationBasedQuery
import com.apollographql.apollo3.benchmark.Utils.registerCacheSize
import com.apollographql.apollo3.benchmark.Utils.resource
import com.apollographql.apollo3.benchmark.Utils.responseBasedQuery
import com.apollographql.apollo3.benchmark.test.R
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.CacheResolver
import com.apollographql.apollo3.cache.normalized.api.DefaultRecordMerger
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyCacheResolver
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.ReadOnlyNormalizedCache
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.normalize
import com.apollographql.apollo3.cache.normalized.api.readDataFromCache
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Method
import java.util.concurrent.Executors

@Suppress("UNCHECKED_CAST")
class CacheIncubatingTests {
  @get:Rule
  val benchmarkRule = BenchmarkRule()

  @Test
  fun cacheOperationMemory() {
    readFromCache("cacheOperationMemory", operationBasedQuery, sql = false, ::checkOperationBased)
  }

  @Test
  fun cacheOperationSql() {
    readFromCache("cacheOperationSql", operationBasedQuery, sql = true, ::checkOperationBased)
  }

  @Test
  fun cacheResponseMemory() {
    readFromCache("cacheResponseMemory", responseBasedQuery, sql = false, ::checkResponseBased)
  }

  @Test
  fun cacheResponseSql() {
    readFromCache("cacheResponseSql", responseBasedQuery, sql = true, ::checkResponseBased)
  }

  @Test
  fun concurrentCacheOperationMemory() {
    concurrentReadWriteFromCache(operationBasedQuery, sql = false)
  }

  @Test
  fun concurrentCacheOperationSql() {
    concurrentReadWriteFromCache(operationBasedQuery, sql = true)
  }

  @Test
  fun concurrentCacheResponseMemory() {
    concurrentReadWriteFromCache(responseBasedQuery, sql = false)
  }

  @Test
  fun concurrentCacheResponseSql() {
    concurrentReadWriteFromCache(responseBasedQuery, sql = true)
  }


  private fun <D : Query.Data> readFromCache(testName: String, query: Query<D>, sql: Boolean, check: (D) -> Unit) {
    val cache = if (sql) {
      Utils.dbFile.delete()
      SqlNormalizedCacheFactory(Utils.dbName, withDates = true).create()
    } else {
      MemoryCacheFactory().create()
    }
    val data = query.parseJsonResponse(resource(R.raw.calendar_response).jsonReader()).data!!

    val records = query.normalize(
        data,
        CustomScalarAdapters.Empty,
        TypePolicyCacheKeyGenerator,
    )

    runBlocking {
      cache.merge(records.values.toList(), CacheHeaders.NONE, DefaultRecordMerger)
    }

    if (sql) {
      registerCacheSize("CacheIncubatingTests", testName, Utils.dbFile.length())
    }
    benchmarkRule.measureRepeated {
      val data2 = query.readDataFromCache(
          CustomScalarAdapters.Empty,
          cache,
          FieldPolicyCacheResolver,
          CacheHeaders.NONE
      )
      check(data2)
    }
  }

  private fun <D : Query.Data> concurrentReadWriteFromCache(query: Query<D>, sql: Boolean) {
    val cache = if (sql) {
      Utils.dbFile.delete()
      SqlNormalizedCacheFactory(Utils.dbName, withDates = true).create()
    } else {
      MemoryCacheFactory().create()
    }
    val data = query.parseJsonResponse(resource(R.raw.calendar_response_simple).jsonReader()).data!!

    val records = query.normalize(
        data,
        CustomScalarAdapters.Empty,
        TypePolicyCacheKeyGenerator,
    )

    val threadPool = Executors.newFixedThreadPool(CONCURRENCY)
    benchmarkRule.measureRepeated {
      val futures = (1..CONCURRENCY).map {
        threadPool.submit {
          // Let each thread execute a few writes/reads
          repeat(WORK_LOAD) {
            cache.merge(records.values.toList(), CacheHeaders.NONE, DefaultRecordMerger)

            val data2 = query.readDataFromCache(
                CustomScalarAdapters.Empty,
                cache,
                FieldPolicyCacheResolver,
                CacheHeaders.NONE
            )

            Assert.assertEquals(data, data2)
          }
        }
      }
      // Wait for all threads to finish
      futures.forEach { it.get() }
    }
  }


  companion object {
    private const val CONCURRENCY = 15
    private const val WORK_LOAD = 15
  }
}