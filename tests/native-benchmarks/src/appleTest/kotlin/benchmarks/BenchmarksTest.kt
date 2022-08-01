package benchmarks

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.native.concurrent.ThreadLocal
import kotlin.test.AfterClass
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.toDuration

@OptIn(ExperimentalTime::class)
@Ignore
class BenchmarksTest {
  private val server = MockServer()
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
          .serverUrl(server.url())
          .build()
    }

    server.enqueue("""
      {
        "data": {
          "random": 42
        }
      }
      """)
    client
        .query(GetRandomQuery())
        .execute()
  }

  private suspend fun simpleQueryWithMemoryCache(iteration: Int) {
    if (iteration == 0) {
      client = ApolloClient.Builder()
          .normalizedCache(MemoryCacheFactory())
          .serverUrl(server.url())
          .build()

      server.enqueue("""
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
  fun benchmarkSimpleQuery() = benchmark("Simple query") { simpleQuery(it) }

  @Test
  fun benchmarkSimpleQueryWithMemoryCache() = benchmark("Simple query with memory cache") { simpleQueryWithMemoryCache(it) }


  data class Measurement(
      val name: String,
      val durations: List<Duration>,
  )

  @ThreadLocal
  companion object {
    private const val EXECUTION_PER_MEASUREMENT = 200
    private const val MEASUREMENT_COUNT = 10

    val measurements = mutableListOf<Measurement>()

    @AfterClass
    fun tearDown() {
      val filePath = "src/measurements".toPath()
      with(FileSystem.SYSTEM) {
        delete(filePath)
        write(filePath, true) {
          for (measurement in measurements) {
            writeUtf8("${measurement.name}:\n")
            writeUtf8("Durations: ${measurement.durations}:\n")
            writeUtf8("Average: ${measurement.durations.map { it.inWholeMilliseconds }.average().toDuration(DurationUnit.MILLISECONDS)}\n\n")
          }
        }
      }
    }
  }
}
