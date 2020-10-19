package com.apollographql.apollo.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.platform.app.InstrumentationRegistry
import com.apollographql.apollo.benchmark.moshi.Query
import com.apollographql.apollo.benchmark.test.R
import com.squareup.moshi.Moshi
import okio.buffer
import okio.source
import org.junit.Rule
import org.junit.Test

class Benchmark {
  @get:Rule
  val benchmarkRule = BenchmarkRule()

  private val operation = GetResponseQuery()

  private val moshiAdapter = Moshi.Builder().build().adapter(Query::class.java)

  private fun bufferedSource() = InstrumentationRegistry.getInstrumentation().context.resources.openRawResource(R.raw.largesample)
      .source()
      .buffer()

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
}