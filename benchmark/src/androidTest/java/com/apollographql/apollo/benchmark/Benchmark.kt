package com.apollographql.apollo.benchmark

import Utils.bufferedSource
import Utils.responseNormalizer
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.platform.app.InstrumentationRegistry
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.api.parse
import com.apollographql.apollo.benchmark.moshi.Query
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.internal.normalize
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter
import com.apollographql.apollo.cache.normalized.internal.ReadableStore
import com.apollographql.apollo.cache.normalized.internal.normalize
import com.apollographql.apollo.cache.normalized.internal.readDataFromCache
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCache
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory
import com.squareup.moshi.Moshi
import okio.Buffer
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

<<<<<<< HEAD
  lateinit var apolloClient: ApolloClient
=======
  lateinit var cache: SqlNormalizedCache
  lateinit var readableStore: ReadableStore
>>>>>>> cacee143... update benchmarks

  @Before
  fun setup() {
    apolloClient = ApolloClient.builder()
        .normalizedCache(SqlNormalizedCacheFactory(context = InstrumentationRegistry.getInstrumentation().context))
        .build()

    val data = operation.parse(bufferedSource()).data!!

<<<<<<< HEAD
    apolloClient.apolloStore.writeOperation(operation, data).execute()
=======
    val records = operation.normalize(data, CustomScalarAdapters.DEFAULT, responseNormalizer)
    cache.merge(records, CacheHeaders.NONE)

    readableStore = object : ReadableStore {
      override fun read(key: String, cacheHeaders: CacheHeaders): Record? {
        TODO("Not yet implemented")
      }

      override fun read(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
        TODO("Not yet implemented")
      }

      override fun stream(key: String, cacheHeaders: CacheHeaders): JsonReader? {
        return cache.stream(key, cacheHeaders)
      }
    }
>>>>>>> cacee143... update benchmarks
  }

  @Test
  fun apolloReadCache() = benchmarkRule.measureRepeated {
    val data2 = operation.readDataFromCache(CustomScalarAdapters.DEFAULT, readableStore, CacheKeyResolver.DEFAULT, CacheHeaders.NONE)
    //println(data2)
  }
}