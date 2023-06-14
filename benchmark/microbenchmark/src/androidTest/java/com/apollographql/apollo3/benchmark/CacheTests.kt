package com.apollographql.apollo3.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import com.apollographql.apollo3.benchmark.Utils.dbFile
import com.apollographql.apollo3.benchmark.Utils.dbName
import com.apollographql.apollo3.benchmark.Utils.operationBasedQuery
import com.apollographql.apollo3.benchmark.Utils.registerCacheSize
import com.apollographql.apollo3.benchmark.Utils.resource
import com.apollographql.apollo3.benchmark.Utils.responseBasedQuery
import com.apollographql.apollo3.benchmark.test.R
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyCacheResolver
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.normalize
import com.apollographql.apollo3.cache.normalized.api.readDataFromCache
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class CacheTests {
  @get:Rule
  val benchmarkRule = BenchmarkRule()

  @Test
  fun cacheOperationMemory() {
    readFromCache("cacheOperationMemory", operationBasedQuery, sql = false, Utils::checkOperationBased)
  }

  @Test
  fun cacheOperationSql() {
    readFromCache("cacheOperationSql", operationBasedQuery, sql = true, Utils::checkOperationBased)
  }

  @Test
  fun cacheResponseMemory() {
    readFromCache("cacheResponseMemory", responseBasedQuery, sql = false, Utils::checkResponseBased)
  }

  @Test
  fun cacheResponseSql() {
    readFromCache("cacheResponseSql", responseBasedQuery, sql = true, Utils::checkResponseBased)
  }

  private fun <D : Query.Data> readFromCache(testName: String, query: Query<D>, sql: Boolean, check: (D) -> Unit) {
    val cache = if (sql) {
      dbFile.delete()
      SqlNormalizedCacheFactory(dbName).create()
    } else {
      MemoryCacheFactory().create()
    }

    val data = query.parseJsonResponse(resource(R.raw.calendar_response).jsonReader()).data!!
    val records = query.normalize(
        data = data,
        customScalarAdapters = CustomScalarAdapters.Empty,
        cacheKeyGenerator = TypePolicyCacheKeyGenerator,
    )

    runBlocking {
      cache.merge(records.values.toList(), CacheHeaders.NONE)
    }

    if (sql) {
      registerCacheSize("CacheTests", testName, dbFile.length())
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
}