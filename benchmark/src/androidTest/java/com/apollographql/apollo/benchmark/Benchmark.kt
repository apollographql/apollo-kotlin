package com.apollographql.apollo.benchmark

import Utils.bufferedSource
import Utils.computeRecordsAfterParsing
import Utils.computeRecordsDuringParsing
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.parse
import com.apollographql.apollo.benchmark.moshi.Query
import com.apollographql.apollo.response.OperationResponseParser
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
  fun apolloParsingWithMapParser() = benchmarkRule.measureRepeated {
    val bufferedSource = runWithTimingDisabled {
      bufferedSource()
    }

    OperationResponseParser(
        operation,
        CustomScalarAdapters.DEFAULT
    ).parse(bufferedSource)
  }

  @Test
  fun apolloComputeRecordsDuringParsing() = benchmarkRule.measureRepeated {
    val bufferedSource = runWithTimingDisabled {
      bufferedSource()
    }

    computeRecordsDuringParsing(operation, bufferedSource)
  }

  @Test
  fun apolloComputeRecordsAfterParsing() = benchmarkRule.measureRepeated {
    val bufferedSource = runWithTimingDisabled {
      bufferedSource()
    }

    computeRecordsAfterParsing(operation, bufferedSource)
  }
}