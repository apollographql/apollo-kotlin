package com.apollographql.apollo.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.parseJsonResponse
import com.apollographql.apollo.benchmark.Utils.getIncubatingMemoryThenSqlCacheFactory
import com.apollographql.apollo.benchmark.Utils.getIncubatingSqlCacheFactory
import com.apollographql.apollo.benchmark.Utils.operationBasedQuery
import com.apollographql.apollo.benchmark.Utils.resource
import com.apollographql.apollo.benchmark.test.R
import com.apollographql.cache.normalized.CacheManager
import com.apollographql.cache.normalized.api.DefaultCacheKeyGenerator
import com.apollographql.cache.normalized.api.DefaultCacheResolver
import com.apollographql.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.cache.normalized.memory.MemoryCacheFactory
import kotlinx.coroutines.runBlocking
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
    concurrentReadWrites(getIncubatingSqlCacheFactory())
  }

  @Test
  fun concurrentReadWritesMemoryThenSql() {
    concurrentReadWrites(getIncubatingMemoryThenSqlCacheFactory())
  }

  private fun concurrentReadWrites(cacheFactory: NormalizedCacheFactory) {
    val apolloStore = createCacheManager(cacheFactory)
    val query = operationBasedQuery
    val data = query.parseJsonResponse(resource(R.raw.calendar_response_simple).jsonReader()).data!!
    val threadPool = Executors.newFixedThreadPool(CONCURRENCY)
    benchmarkRule.measureRepeated {
      val futures = (1..CONCURRENCY).map {
        threadPool.submit {
          // Let each thread execute a few writes/reads
          repeat(WORK_LOAD) {
            runBlocking {
              apolloStore.writeOperation(query, data)
              val data2 = apolloStore.readOperation(query).data
              Assert.assertEquals(data, data2)
            }
          }
        }
      }
      // Wait for all threads to finish
      futures.forEach { it.get() }
    }
  }

  private fun createCacheManager(cacheFactory: NormalizedCacheFactory): CacheManager {
    return CacheManager(cacheFactory, cacheKeyGenerator = DefaultCacheKeyGenerator, cacheResolver = DefaultCacheResolver)
  }


  companion object {
    private const val CONCURRENCY = 10
    private const val WORK_LOAD = 5
  }
}
