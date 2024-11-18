package com.apollographql.apollo.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.parseJsonResponse
import com.apollographql.apollo.benchmark.Utils.dbName
import com.apollographql.apollo.benchmark.Utils.operationBasedQuery
import com.apollographql.apollo.benchmark.Utils.resource
import com.apollographql.apollo.benchmark.test.R
import com.apollographql.cache.normalized.ApolloStore
import com.apollographql.cache.normalized.memory.MemoryCacheFactory
import com.apollographql.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.cache.normalized.sql.SqlNormalizedCacheFactory
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.Executors

class ApolloStoreIncubatingTests {
  @get:Rule
  val benchmarkRule = BenchmarkRule()

  @Test
  fun concurrentReadWritesMemory() {
    concurrentReadWrites(MemoryCacheFactory())
  }

  @Test
  fun concurrentReadWritesSql() {
    Utils.dbFile.delete()
    val cacheFactory = SqlNormalizedCacheFactory(dbName)
    concurrentReadWrites(cacheFactory)
  }

  @Test
  fun concurrentReadWritesMemoryThenSql() {
    Utils.dbFile.delete()
    val cacheFactory = MemoryCacheFactory().chain(SqlNormalizedCacheFactory(dbName))
    concurrentReadWrites(cacheFactory)
  }

  private fun concurrentReadWrites(cacheFactory: NormalizedCacheFactory) {
    val apolloStore = createApolloStore(cacheFactory)
    val query = operationBasedQuery
    val data = query.parseJsonResponse(resource(R.raw.calendar_response_simple).jsonReader()).data!!
    val threadPool = Executors.newFixedThreadPool(CONCURRENCY)
    benchmarkRule.measureRepeated {
      val futures = (1..CONCURRENCY).map {
        threadPool.submit {
          // Let each thread execute a few writes/reads
          repeat(WORK_LOAD) {
            apolloStore.writeOperation(query, data)
            val data2 = apolloStore.readOperation(query).data
            Assert.assertEquals(data, data2)
          }
        }
      }
      // Wait for all threads to finish
      futures.forEach { it.get() }
    }
  }

  private fun createApolloStore(cacheFactory: NormalizedCacheFactory): ApolloStore {
    return ApolloStore(cacheFactory)
  }


  companion object {
    private const val CONCURRENCY = 10
    private const val WORK_LOAD = 5
  }
}
