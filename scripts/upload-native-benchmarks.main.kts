#!/usr/bin/env kotlin

@file:DependsOn("com.squareup.moshi:moshi-kotlin:1.13.0")
@file:DependsOn("com.squareup.okhttp3:okhttp:4.10.0")

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer

val moshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

data class Benchmarks(
    val benchmarks: List<Benchmark>,
)

data class Benchmark(
    val name: String,
    val measurements: List<Long>,
)

fun loadBenchmarkResults(): Benchmarks {
  return moshi.adapter(Benchmarks::class.java)
      .fromJson(FileSystem.SYSTEM.source("tests/native-benchmarks/build/measurements.json".toPath()).buffer())!!
}

data class Point(
    val timestamp: Long,
    val value: Long,
)

data class Series(
    val metric: String,
    val type: Int,
    val points: List<Point>,
)

data class SeriesList(
    val series: List<Series>,
)

fun Benchmarks.toSeriesList(): SeriesList {
  val now = System.currentTimeMillis() / 1000
  return SeriesList(
      benchmarks.map { benchmark ->
        Series(
            metric = benchmark.name,
            type = 0,
            points = benchmark.measurements.map { measurement -> Point(timestamp = now, value = measurement) },
        )
      }
  )
}

fun main() {
  val seriesList = loadBenchmarkResults().toSeriesList()
  val body = moshi.adapter(SeriesList::class.java).toJson(seriesList)
  val request = Request.Builder().url("https://api.datadoghq.com/api/v2/series")
      .post(body.toRequestBody("application/json".toMediaType()))
      .addHeader("DD-API-KEY", System.getenv("DD_API_KEY"))
      .build()
  val response = OkHttpClient.Builder().build()
      .newCall(request)
      .execute()

  check(response.isSuccessful) {
    "Cannot post to Datadog: '${response.code}'\n${response.body?.string()}"
  }
  println("posted to Datadog")
}

main()
