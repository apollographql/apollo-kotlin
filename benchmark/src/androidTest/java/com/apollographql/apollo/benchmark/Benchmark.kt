package com.apollographql.apollo.benchmark

import Utils
import Utils.bufferedSource
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.parse
import com.apollographql.apollo.benchmark.moshi.Query
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.internal.normalize
import com.squareup.moshi.Moshi
import org.junit.Rule
import org.junit.Test

class Benchmark {
  @get:Rule
  val benchmarkRule = BenchmarkRule()

  private val operation = GetResponseQuery()

  private val moshiAdapter = Moshi.Builder().build().adapter(Query::class.java)

  @Test
  fun apollo() = benchmarkRule.measureRepeated {
    val bufferedSource = runWithTimingDisabled {
      bufferedSource()
    }

    operation.parse(bufferedSource)
  }

  @Test
  fun moshi() = benchmarkRule.measureRepeated {
    val bufferedSource = runWithTimingDisabled {
      bufferedSource()
    }

    moshiAdapter.fromJson(bufferedSource)
  }


  @Test
  fun apolloParseAndNormalize() = benchmarkRule.measureRepeated {
    val bufferedSource = runWithTimingDisabled {
      bufferedSource()
    }

    val data = operation.parse(bufferedSource).data!!
    val records = operation.normalize(data, CustomScalarAdapters.DEFAULT, CacheKeyResolver.DEFAULT)
  }
}