package com.apollographql.apollo.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.parseJsonResponse
import com.apollographql.apollo.benchmark.Utils.dbFile
import com.apollographql.apollo.benchmark.Utils.dbName
import com.apollographql.apollo.benchmark.Utils.operationBasedQuery
import com.apollographql.apollo.benchmark.Utils.registerCacheSize
import com.apollographql.apollo.benchmark.Utils.resource
import com.apollographql.apollo.benchmark.Utils.responseBasedQuery
import com.apollographql.apollo.benchmark.test.R
import com.apollographql.cache.normalized.ApolloStore
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
    val store = ApolloStore(
        if (sql) {
          dbFile.delete()
          SqlNormalizedCacheFactory(name = dbName)
        } else {
          MemoryCacheFactory()
        }
    )

    val data = query.parseJsonResponse(resource(R.raw.calendar_response).jsonReader()).data!!
    runBlocking {
      store.writeOperation(query, data)
    }

    if (sql) {
      registerCacheSize("CacheIncubatingTests", testName, dbFile.length())
    }
    benchmarkRule.measureRepeated {
      val data2 = store.readOperation(query).data!!
      check(data2)
    }
  }
}
