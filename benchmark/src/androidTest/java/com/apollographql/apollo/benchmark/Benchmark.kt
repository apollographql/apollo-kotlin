package com.apollographql.apollo.benchmark

import Utils.bufferedSource
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.platform.app.InstrumentationRegistry
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.parse
import com.apollographql.apollo.benchmark.moshi.Query
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.internal.normalize
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory
import com.squareup.moshi.Moshi
import org.junit.Before
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

  lateinit var apolloClient: ApolloClient

  @Before
  fun setup() {
    apolloClient = ApolloClient.builder()
        .normalizedCache(SqlNormalizedCacheFactory(context = InstrumentationRegistry.getInstrumentation().context))
        .build()

    val data = operation.parse(bufferedSource()).data!!

    apolloClient.apolloStore.writeOperation(operation, data)
  }

  @Test
  fun apolloReadCache() = benchmarkRule.measureRepeated {

    val data = apolloClient.apolloStore.readOperation(operation).execute()
    println(data)
  }
}