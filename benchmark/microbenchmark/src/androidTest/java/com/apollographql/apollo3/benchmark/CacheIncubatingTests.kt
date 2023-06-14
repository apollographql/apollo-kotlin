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
import com.apollographql.apollo3.cache.normalized.incubating.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.incubating.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.incubating.api.CacheResolver
import com.apollographql.apollo3.cache.normalized.incubating.api.FieldPolicyCacheResolver
import com.apollographql.apollo3.cache.normalized.incubating.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.incubating.api.ReadOnlyNormalizedCache
import com.apollographql.apollo3.cache.normalized.incubating.api.Record
import com.apollographql.apollo3.cache.normalized.incubating.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.incubating.sql.SqlNormalizedCacheFactory
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Method

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

  private fun <D : Query.Data> readFromCache(testName: String, query: Query<D>, sql: Boolean, check: (D) -> Unit) {
    val cache = if (sql) {
      MemoryCacheFactory().create()
    } else {
      Utils.dbFile.delete()
      // Pass context explicitly here because androidx.startup fails due to relocation
      SqlNormalizedCacheFactory(InstrumentationRegistry.getInstrumentation().context, Utils.dbName).create()
    }
    val data = query.parseJsonResponse(resource(R.raw.calendar_response).jsonReader()).data!!

    val records = normalizeMethod.invoke(
        null,
        query,
        data,
        CustomScalarAdapters.Empty,
        TypePolicyCacheKeyGenerator,
    ) as Map<String, Record>

    runBlocking {
      cache.merge(records.values.toList(), CacheHeaders.NONE)
    }

    if (sql) {
      registerCacheSize("CacheIncubatingTests", testName, Utils.dbFile.length())
    }
    benchmarkRule.measureRepeated {
      val data2 = readDataFromCacheMethod.invoke(
          null,
          query,
          CustomScalarAdapters.Empty,
          cache,
          FieldPolicyCacheResolver,
          CacheHeaders.NONE
      ) as D
      check(data2)
    }
  }

  companion object {
    /**
     * There doesn't seem to be a way to relocate Kotlin metdata and kotlin_module files so we rely on reflection to call top-level
     * methods
     * See https://discuss.kotlinlang.org/t/what-is-the-proper-way-to-repackage-shade-kotlin-dependencies/10869
     */
    private val clazz = Class.forName("com.apollographql.apollo3.cache.normalized.incubating.api.OperationCacheExtensionsKt")
    private val normalizeMethod: Method = clazz.getMethod(
        "normalize",
        Operation::class.java,
        Operation.Data::class.java,
        CustomScalarAdapters::class.java,
        CacheKeyGenerator::class.java
    )

    private val readDataFromCacheMethod: Method = clazz.getMethod(
        "readDataFromCache",
        Executable::class.java,
        CustomScalarAdapters::class.java,
        ReadOnlyNormalizedCache::class.java,
        CacheResolver::class.java,
        CacheHeaders::class.java
    )
  }
}