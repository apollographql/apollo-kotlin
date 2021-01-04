package com.apollographql.apollo.performance

import com.apollographql.apollo.api.parse
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.Utils.immediateExecutor
import com.apollographql.apollo.Utils.immediateExecutorService
import com.apollographql.apollo.api.internal.SimpleOperationResponseParser
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.fetcher.ApolloResponseFetchers
import com.apollographql.apollo.integration.performance.GetFloatsQuery
import com.apollographql.apollo.integration.performance.GetIntsQuery
import kotlinx.coroutines.runBlocking
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import kotlin.random.Random
import kotlin.test.Test
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

data class Data(
    val randomInts: List<Int>
)

/**
 * These tests are disabled by default. Set the `runPerformanceTests` property to run them:
 * ./gradlew :apollo-integration:testDebug --tests '*parseFloats*' -DrunPerformanceTests=true
 */
@OptIn(ExperimentalTime::class)
class NumberParsingTest {
  private fun mockJson(block: (JsonWriter) -> Unit): ByteString {
    val buffer = Buffer()
    val writer = JsonWriter.of(buffer)
    writer.beginObject()
    writer.name("data")
    writer.beginObject()
    writer.name("randomFloats")
    writer.beginArray()
    repeat(10000) {
      block(writer)
    }
    writer.endArray()
    writer.endObject()
    writer.endObject()

    return buffer.readByteString()
  }

  /**
   * A test to benchmark the parsing of integers in Json.
   */
  @Test
  fun parseInts() {
    val random = Random.Default
    val json = mockJson { it.jsonValue(random.nextDouble().toString()) }

    val time = measureTime {
      val operation = GetIntsQuery()
      repeat(10000) {
        operation.parse(json)
      }
    }
    println("parseInts: ${time}")
  }

  /**
   * A test to benchmark the parsing of floats in Json. At the time of writing, it takes ~21s on my MacBook
   *
   * From the first tests, Switching to Double instead of BigDecimal isn't way faster but makes a bit less allocations
   * Number of GC is down to 228 with Double (from 274 with BigDecimal) so it might be worth at some point.
   */
  @Test
  fun parseFloats() {

    val random = Random.Default
    val json = mockJson { it.jsonValue(random.nextDouble().toString()) }

    Runtime.getRuntime().gc()
    val time = measureTime {
      val operation = GetFloatsQuery()
      repeat(4000) {
        operation.parse(json)
      }
    }
    Runtime.getRuntime().gc()
    println("parseFloats: ${time}")
  }
}
