package com.apollographql.apollo3.performance

import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.parseJsonResponse
import com.apollographql.apollo3.integration.performance.GetFloatsQuery
import com.apollographql.apollo3.integration.performance.GetIntsQuery
import okio.Buffer
import okio.ByteString
import kotlin.random.Random
import kotlin.test.Test
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

data class Data(
    val randomInts: List<Int>
)

/**
 * These tests are disabled by default. Set the `runPerformanceTests` property to run them:
 * ./gradlew :integration-tests:testDebug --tests '*parseFloats*' -DrunPerformanceTests=true
 */
@OptIn(ExperimentalTime::class)
class NumberParsingTest {
  private fun mockJson(block: (JsonWriter) -> Unit): ByteString {
    val buffer = Buffer()
    val writer = BufferedSinkJsonWriter(buffer)
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
    val json = mockJson { it.value(random.nextDouble()) }

    val time = measureTime {
      val operation = GetIntsQuery()
      repeat(10000) {
        operation.parseJsonResponse(json)
      }
    }
    println("parseInts: $time")
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
    val json = mockJson { it.value(random.nextDouble()) }

    Runtime.getRuntime().gc()
    val time = measureTime {
      val operation = GetFloatsQuery()
      repeat(4000) {
        operation.parseJsonResponse(json)
      }
    }
    Runtime.getRuntime().gc()
    println("parseFloats: $time")
  }
}
