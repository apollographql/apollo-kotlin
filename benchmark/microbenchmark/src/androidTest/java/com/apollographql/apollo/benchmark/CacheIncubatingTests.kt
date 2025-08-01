package com.apollographql.apollo.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.parseJsonResponse
import com.apollographql.apollo.benchmark.Utils.dbFile
import com.apollographql.apollo.benchmark.Utils.dbName
import com.apollographql.apollo.benchmark.Utils.largeListQuery
import com.apollographql.apollo.benchmark.Utils.operationBasedQuery
import com.apollographql.apollo.benchmark.Utils.registerCacheSize
import com.apollographql.apollo.benchmark.Utils.resource
import com.apollographql.apollo.benchmark.Utils.responseBasedQuery
import com.apollographql.apollo.benchmark.test.R
import com.apollographql.cache.normalized.CacheManager
import com.apollographql.cache.normalized.memory.MemoryCacheFactory
import com.apollographql.cache.normalized.sql.SqlNormalizedCacheFactory
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class CacheIncubatingTests {
  @get:Rule
  val benchmarkRule = BenchmarkRule()

  @Test
  fun cacheOperationMemory() {
    readFromCache("cacheOperationMemory", operationBasedQuery, R.raw.calendar_response, sql = false, Utils::checkOperationBased)
  }

  @Test
  fun cacheOperationSql() {
    readFromCache("cacheOperationSql", operationBasedQuery, R.raw.calendar_response, sql = true, Utils::checkOperationBased)
  }

  @Test
  fun cacheResponseMemory() {
    readFromCache("cacheResponseMemory", responseBasedQuery, R.raw.calendar_response, sql = false, Utils::checkResponseBased)
  }

  @Test
  fun cacheResponseSql() {
    readFromCache("cacheResponseSql", responseBasedQuery, R.raw.calendar_response, sql = true, Utils::checkResponseBased)
  }

  @Test
  fun cacheLargeListMemory() {
    readFromCache("cacheLargeListMemory", largeListQuery, R.raw.tracks_playlist_response, sql = false, Utils::checkLargeList)
  }

  @Test
  fun cacheLargeListSql() {
    readFromCache("cacheLargeListSql", largeListQuery, R.raw.tracks_playlist_response, sql = true, Utils::checkLargeList)
  }

  private fun <D : Query.Data> readFromCache(testName: String, query: Query<D>, jsonResponseResId: Int, sql: Boolean, check: (D) -> Unit) {
    val cacheManager = CacheManager(
        if (sql) {
          dbFile.delete()
          SqlNormalizedCacheFactory(name = dbName)
        } else {
          MemoryCacheFactory()
        }
    )

    val data = query.parseJsonResponse(resource(jsonResponseResId).jsonReader()).data!!
    runBlocking {
      cacheManager.writeOperation(query, data)
    }

    if (sql) {
      registerCacheSize("CacheIncubatingTests", testName, dbFile.length())
    }
    benchmarkRule.measureRepeated {
      runBlocking {
        val data2 = cacheManager.readOperation(query).data!!
        check(data2)
      }
    }
  }
}
