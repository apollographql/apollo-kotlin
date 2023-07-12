package com.apollographql.apollo3.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import com.apollographql.apollo3.benchmark.Utils.dbFile
import com.apollographql.apollo3.benchmark.Utils.dbName
import com.apollographql.apollo3.benchmark.Utils.registerCacheSize
import com.apollographql.apollo3.benchmark.Utils.resource
import com.apollographql.apollo3.benchmark.test.R
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyCacheResolver
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.NormalizedCache
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.normalize
import com.apollographql.apollo3.cache.normalized.api.readDataFromCache
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import com.apollographql.apollo3.benchmark.GetResponseQuery as Query1
import com.apollographql.apollo3.benchmark2.GetResponseQuery as Query2

class CacheTests2 {
  val query1 = Query1()
  val query2 = Query2()

  @get:Rule
  val benchmarkRule = BenchmarkRule()


  @Test
  fun query1Memory() {
    readFromCache("query1Memory", query1, sql = false) {
      check(it.users[59].images[11].url == "http://ourimageserver/f5a3803a-8d97-417e-ad28-1be3a3e89820")
    }
  }

  @Test
  fun query1Sql() {
    readFromCache("query1Sql", query1, sql = true) {
      check(it.users[59].images[11].url == "http://ourimageserver/f5a3803a-8d97-417e-ad28-1be3a3e89820")
    }
  }

  @Test
  fun query2Memory() {
    readFromCache("query2Memory", query2, sql = false) {
      check(it.users[59].images[11].url == "http://ourimageserver/f5a3803a-8d97-417e-ad28-1be3a3e89820")
    }
  }

  @Test
  fun query2Sql() {
    readFromCache("query2Sql", query2, sql = true) {
      check(it.users[59].images[11].url == "http://ourimageserver/f5a3803a-8d97-417e-ad28-1be3a3e89820")
    }
  }

  private fun <D : Query.Data> readFromCache(testName: String, query: Query<D>, sql: Boolean, check: (D)->Unit) {
    val cache: NormalizedCache = if (sql) {
      dbFile.delete()
      SqlNormalizedCacheFactory(dbName).create()
    } else {
      MemoryCacheFactory().create()
    }

    val data = query.parseJsonResponse(resource(R.raw.largesample).jsonReader()).data!!
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