@file:Suppress("DEPRECATION")

package benchmark.native

import benchmarks.GetRandomQuery
import benchmarks.builder.Data
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.toResponseJson
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.testing.internal.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.collections.mapIndexed
import kotlin.test.AfterClass
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.measureTime

class BenchmarksTest {
  private val mockServer = MockServer()
  private lateinit var client: ApolloClient

  private fun benchmark(testName: String, test: suspend (Int) -> Unit) = runTest {
    val durations = mutableListOf<Duration>()
    repeat(MEASUREMENT_COUNT) {
      durations.add(
          measureTime {
            repeat(EXECUTION_PER_MEASUREMENT) { test(it) }
          }
      )
    }
    testToNanos.put(testName, durations.map { it.toLong(DurationUnit.NANOSECONDS) }.average())
  }

  private suspend fun simpleQuery(iteration: Int) {
    if (iteration == 0) {
      client = ApolloClient.Builder()
          .serverUrl(mockServer.url())
          .build()
    }

    mockServer.enqueueString(
        GetRandomQuery.Data {
          random = 42
        }
            .toResponseJson())
    client
        .query(GetRandomQuery())
        .execute()
  }

  private suspend fun simpleQueryWithMemoryCache(iteration: Int) {
    if (iteration == 0) {
      client = ApolloClient.Builder()
          .normalizedCache(MemoryCacheFactory())
          .serverUrl(mockServer.url())
          .build()

      mockServer.enqueueString("""
      {
        "data": {
          "random": 42
        }
      }
      """)
    }

    client
        .query(GetRandomQuery())
        .execute()
  }

  @Test
  fun benchmarkSimpleQuery() = benchmark("benchmarkSimpleQuery") { simpleQuery(it) }

  @Test
  fun benchmarkSimpleQueryWithMemoryCache() = benchmark("benchmarkSimpleQueryWithMemoryCache") { simpleQueryWithMemoryCache(it) }

  companion object {
    private const val EXECUTION_PER_MEASUREMENT = 500
    private const val MEASUREMENT_COUNT = 10

    val testToNanos = mutableMapOf<String, Double>()

    @AfterClass
    fun tearDown() {
      val filePath = "build/measurements.json".toPath()
      with(FileSystem.SYSTEM) {
        delete(filePath)
        write(filePath, true) {
          writeUtf8("""{"benchmarks":[""")
          testToNanos.toList().mapIndexed { index, it ->
            writeUtf8("""{"class":"${BenchmarksTest::class.qualifiedName!!}",""")
            writeUtf8(""""test":"${it.first}",""")
            writeUtf8(""""nanos":${it.second}} """)
            if (index != testToNanos.size - 1) {
              writeUtf8(",")
            }
          }
          writeUtf8("]}")
        }
      }
    }
  }
}
