package benchmarks

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.toResponseJson
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueueString
import com.apollographql.apollo3.testing.internal.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.AfterClass
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.measureTime

class BenchmarksTest {
  private val mockServer = MockServer()
  private lateinit var client: ApolloClient

  private fun benchmark(name: String, test: suspend (Int) -> Unit) = runTest {
    val durations = mutableListOf<Duration>()
    repeat(MEASUREMENT_COUNT) {
      durations.add(
          measureTime {
            repeat(EXECUTION_PER_MEASUREMENT) { test(it) }
          }
      )
    }
    measurements.add(Measurement(name, durations))
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
  fun benchmarkSimpleQuery() = benchmark("apollo.kotlin.native.simplequery.nocache") { simpleQuery(it) }

  @Test
  fun benchmarkSimpleQueryWithMemoryCache() = benchmark("apollo.kotlin.native.simplequery.memorycache") { simpleQueryWithMemoryCache(it) }


  data class Measurement(
      val name: String,
      val durations: List<Duration>,
  )

  companion object {
    private const val EXECUTION_PER_MEASUREMENT = 500
    private const val MEASUREMENT_COUNT = 10

    val measurements = mutableListOf<Measurement>()

    @AfterClass
    fun tearDown() {
      val filePath = "build/measurements.json".toPath()
      with(FileSystem.SYSTEM) {
        delete(filePath)
        write(filePath, true) {
          writeUtf8("""{"benchmarks":[""")
          for ((i, measurement) in measurements.withIndex()) {
            writeUtf8("""{"name":"${measurement.name}","measurements":[""")
            writeUtf8(measurement.durations.map { it.inWholeMilliseconds }.joinToString(","))
            writeUtf8("]}")
            if (i != measurements.lastIndex) {
              writeUtf8(",")
            }
          }
          writeUtf8("]}")
        }
      }
    }
  }
}
