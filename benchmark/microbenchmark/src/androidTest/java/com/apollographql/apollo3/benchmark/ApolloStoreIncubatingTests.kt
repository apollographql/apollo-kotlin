package com.apollographql.apollo3.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.platform.app.InstrumentationRegistry
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import com.apollographql.apollo3.benchmark.Utils.dbName
import com.apollographql.apollo3.benchmark.Utils.operationBasedQuery
import com.apollographql.apollo3.benchmark.Utils.resource
import com.apollographql.apollo3.benchmark.test.R
import com.apollographql.apollo3.cache.normalized.incubating.ApolloStore
import com.apollographql.apollo3.cache.normalized.incubating.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.incubating.api.CacheResolver
import com.apollographql.apollo3.cache.normalized.incubating.api.FieldPolicyCacheResolver
import com.apollographql.apollo3.cache.normalized.incubating.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.incubating.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.incubating.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.incubating.sql.SqlNormalizedCacheFactory
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Method
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
    // Pass context explicitly here because androidx.startup fails due to relocation
    val cacheFactory = SqlNormalizedCacheFactory(InstrumentationRegistry.getInstrumentation().context, dbName)
    concurrentReadWrites(cacheFactory)
  }

  @Test
  fun concurrentReadWritesMemoryThenSql() {
    Utils.dbFile.delete()
    val cacheFactory = MemoryCacheFactory().chain(SqlNormalizedCacheFactory(InstrumentationRegistry.getInstrumentation().context, dbName))
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
            val data2 = apolloStore.readOperation(query)
            Assert.assertEquals(data, data2)
          }
        }
      }
      // Wait for all threads to finish
      futures.forEach { it.get() }
    }
  }

  private fun createApolloStore(cacheFactory: NormalizedCacheFactory): ApolloStore {
    return createApolloStoreMethod.invoke(
        null,
        cacheFactory,
        TypePolicyCacheKeyGenerator,
        FieldPolicyCacheResolver,
    ) as ApolloStore
  }


  companion object {
    private const val CONCURRENCY = 10
    private const val WORK_LOAD = 5

    /**
     * There doesn't seem to be a way to relocate Kotlin metadata and kotlin_module files so we rely on reflection to call top-level
     * methods
     * See https://discuss.kotlinlang.org/t/what-is-the-proper-way-to-repackage-shade-kotlin-dependencies/10869
     */
    private val apolloStoreKtClass = Class.forName("com.apollographql.apollo3.cache.normalized.incubating.ApolloStoreKt")
    private val createApolloStoreMethod: Method = apolloStoreKtClass.getMethod(
        "ApolloStore",
        NormalizedCacheFactory::class.java,
        CacheKeyGenerator::class.java,
        CacheResolver::class.java,
    )
  }
}
