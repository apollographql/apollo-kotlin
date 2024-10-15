package com.apollographql.apollo.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.parseJsonResponse
import com.apollographql.apollo.benchmark.Utils.checkOperationBased
import com.apollographql.apollo.benchmark.Utils.checkResponseBased
import com.apollographql.apollo.benchmark.Utils.operationBasedQuery
import com.apollographql.apollo.benchmark.Utils.registerCacheSize
import com.apollographql.apollo.benchmark.Utils.resource
import com.apollographql.apollo.benchmark.Utils.responseBasedQuery
import com.apollographql.apollo.benchmark.test.R
import com.apollographql.cache.normalized.api.CacheHeaders
import com.apollographql.cache.normalized.api.DefaultRecordMerger
import com.apollographql.cache.normalized.api.FieldPolicyCacheResolver
import com.apollographql.cache.normalized.memory.MemoryCacheFactory
import com.apollographql.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.cache.normalized.api.normalize
import com.apollographql.cache.normalized.api.readDataFromCache
import com.apollographql.cache.normalized.sql.SqlNormalizedCacheFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.Executors

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
      SqlNormalizedCacheFactory(name = Utils.dbName).create()
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
      SqlNormalizedCacheFactory(name = Utils.dbName).create()
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
