package com.apollographql.apollo3.benchmark

import Utils.resource
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import com.apollographql.apollo3.benchmark.test.R
import com.apollographql.apollo3.cache.normalized.incubating.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.incubating.api.FieldPolicyCacheResolver
import com.apollographql.apollo3.cache.normalized.incubating.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.incubating.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.incubating.api.normalize
import com.apollographql.apollo3.cache.normalized.incubating.api.readDataFromCache
import com.apollographql.apollo3.calendar.response.ItemsQuery as ItemsQueryResponseBased
import com.apollographql.apollo3.calendar.operation.ItemsQuery as ItemsQueryOperationBased
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

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

  fun <D: Query.Data> readFromCache(query: Query<D>) {
    val cache = MemoryCacheFactory().create()

    val data = query.parseJsonResponse(resource(R.raw.calendar_response).jsonReader()).data!!

    com.apollographql.apollo3.cache.normalized.incubating.api.OperationCacheExtensionsKt
    val records = query.normalize(
        data = data,
        customScalarAdapters = CustomScalarAdapters.Empty,
        cacheKeyGenerator = TypePolicyCacheKeyGenerator,
    )

    runBlocking {
      cache.merge(records.values.toList(), CacheHeaders.NONE)
    }

    benchmarkRule.measureRepeated {
      query.readDataFromCache(
          CustomScalarAdapters.Empty,
          cache,
          FieldPolicyCacheResolver,
          CacheHeaders.NONE
      )
    }
  }
}