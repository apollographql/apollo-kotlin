package com.apollographql.apollo.performance

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.Utils.immediateExecutor
import com.apollographql.apollo.Utils.immediateExecutorService
import com.apollographql.apollo.api.internal.SimpleOperationResponseParser
import com.apollographql.apollo.fetcher.ApolloResponseFetchers
import com.apollographql.apollo.integration.performance.GetIntsQuery
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import kotlin.random.Random
import kotlin.test.Test
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

data class Data(
    val randomInts: List<Int>
)

class NumberParsingTest {
  @OptIn(ExperimentalTime::class)
  @Test
  fun parseInts() {

    val random = Random.Default

    val data = JsonObject(
        mapOf(
            "data" to JsonObject(
                mapOf(
                    "randomInts" to JsonArray(
                        0.until(10000).map {
                          JsonPrimitive(random.nextInt())
                        }
                    )
                )
            )
        )
    )

    val json = data.toString().encodeUtf8()

    val time = measureTime {
      val operation = GetIntsQuery()
      repeat(10000) {
        val data = operation.parse(json)
        //println(data)
      }
    }
    println("IntParsing: ${time}")
  }
}