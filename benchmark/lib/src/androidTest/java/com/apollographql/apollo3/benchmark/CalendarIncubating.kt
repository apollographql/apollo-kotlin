package com.apollographql.apollo3.benchmark

import Utils.resource
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import com.apollographql.apollo3.benchmark.test.R
import com.apollographql.apollo3.cache.normalized.incubating.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.incubating.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.incubating.api.CacheResolver
import com.apollographql.apollo3.cache.normalized.incubating.api.FieldPolicyCacheResolver
import com.apollographql.apollo3.cache.normalized.incubating.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.incubating.api.ReadOnlyNormalizedCache
import com.apollographql.apollo3.cache.normalized.incubating.api.Record
import com.apollographql.apollo3.cache.normalized.incubating.api.TypePolicyCacheKeyGenerator
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import com.apollographql.apollo3.calendar.operation.ItemsQuery as ItemsQueryOperationBased
import com.apollographql.apollo3.calendar.response.ItemsQuery as ItemsQueryResponseBased

class CalendarIncubating {
  @get:Rule
  val benchmarkRule = BenchmarkRule()

  @Test
  fun readFromCacheResponseBasedIncubating() {
    readFromCache(ItemsQueryResponseBased(endingAfter = "", startingBefore = ""))
  }

  @Test
  fun readFromCacheOperationBasedIncubating() {
    readFromCache(ItemsQueryOperationBased(endingAfter = "", startingBefore = ""))
  }

  fun <D : Query.Data> readFromCache(query: Query<D>) {
    val cache = MemoryCacheFactory().create()

    val data = query.parseJsonResponse(resource(R.raw.calendar_response).jsonReader()).data!!

    /**
     * There doesn't seem to be a way to relocate Kotlin metdata and kotlin_module files so we rely on reflection to call top-level
     * methods
     * See https://discuss.kotlinlang.org/t/what-is-the-proper-way-to-repackage-shade-kotlin-dependencies/10869
     */
    val clazz = Class.forName("com.apollographql.apollo3.cache.normalized.incubating.api.OperationCacheExtensionsKt")
    clazz.methods.forEach {
      println("method: $it")
    }
    val normalizeMethod = clazz.getMethod(
        "normalize",
        Operation::class.java,
        Operation.Data::class.java,
        CustomScalarAdapters::class.java,
        CacheKeyGenerator::class.java
    )
    val readDataFromCacheMethod = clazz.getMethod(
        "readDataFromCache",
        Executable::class.java,
        CustomScalarAdapters::class.java,
        ReadOnlyNormalizedCache::class.java,
        CacheResolver::class.java,
        CacheHeaders::class.java
    )
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

    benchmarkRule.measureRepeated {
      readDataFromCacheMethod.invoke(
          null,
          query,
          CustomScalarAdapters.Empty,
          cache,
          FieldPolicyCacheResolver,
          CacheHeaders.NONE
      )
    }
  }
}