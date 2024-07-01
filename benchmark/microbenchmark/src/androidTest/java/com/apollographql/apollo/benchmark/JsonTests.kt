package com.apollographql.apollo.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.parseJsonResponse
import com.apollographql.apollo.benchmark.Utils.resource
import com.apollographql.apollo.benchmark.moshi.Query
import com.apollographql.apollo.benchmark.test.R
import com.apollographql.apollo.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo.cache.normalized.api.normalize
import com.squareup.moshi.Moshi
import org.junit.Rule
import org.junit.Test

class JsonTests {
  @get:Rule
  val benchmarkRule = BenchmarkRule()

  @Test
  fun jsonMoshi() = benchmarkRule.measureRepeated {
    val response = moshiAdapter.fromJson(resource(R.raw.largesample))
    check(response!!.data.users[59].images[11].url == "http://ourimageserver/f5a3803a-8d97-417e-ad28-1be3a3e89820")
  }

  @Test
  fun jsonApollo() = benchmarkRule.measureRepeated {
    val response = operation.parseJsonResponse(resource(R.raw.largesample).jsonReader(), customScalarAdapters)
    check(response.data!!.users[59].images[11].url == "http://ourimageserver/f5a3803a-8d97-417e-ad28-1be3a3e89820")
  }

  @Test
  fun normalizeApollo() = benchmarkRule.measureRepeated {
    val data = runWithTimingDisabled {
      operation.parseJsonResponse(resource(R.raw.largesample).jsonReader(), customScalarAdapters).data!!
    }
    operation.normalize(data, customScalarAdapters, TypePolicyCacheKeyGenerator)
  }

  companion object {
    private val operation = GetResponseQuery()
    private val moshiAdapter = Moshi.Builder().build().adapter(Query::class.java)
    private val customScalarAdapters = CustomScalarAdapters.Empty
  }
}