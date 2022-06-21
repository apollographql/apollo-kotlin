package com.apollographql.apollo3.benchmark

import Utils.resource
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import com.apollographql.apollo3.benchmark.test.R
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyCacheResolver
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.normalize
import com.apollographql.apollo3.cache.normalized.api.readDataFromCache
import com.apollographql.apollo3.calendar.response.ItemsQuery
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class Calendar {
  @get:Rule
  val benchmarkRule = BenchmarkRule()

  @Test
  fun responseBasedReadFromCache() {
    val query = ItemsQuery(endingAfter = "", startingBefore = "")
    val cache = MemoryCacheFactory().create()

    val data = query.parseJsonResponse(resource(R.raw.calendar_response).jsonReader()).data!!

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