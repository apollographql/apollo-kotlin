#!/usr/bin/env kotlin

@file:DependsOn("net.mbonnin.bare-graphql:bare-graphql:0.0.2")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.6.2")
@file:DependsOn("com.squareup.okhttp3:okhttp:4.10.0")

import kotlinx.serialization.json.Json
import net.mbonnin.bare.graphql.asList
import net.mbonnin.bare.graphql.asMap
import net.mbonnin.bare.graphql.asNumber
import net.mbonnin.bare.graphql.asString
import net.mbonnin.bare.graphql.graphQL
import net.mbonnin.bare.graphql.toAny
import net.mbonnin.bare.graphql.toJsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.Date

/**
 * Uploads the benchmarks results to Datadog and updates the benchmarks dashboard issue.
 *
 * Several kinds of results are read:
 *
 * - the micro benchmarks written by `scripts/run-firebase-benchmarks.main.kts` in [firebaseResultsFile]
 * - the native benchmarks written by `./gradlew -p tests :native-benchmarks:allTests` in [nativeResultsFile]
 * - the AST (JMH) benchmarks written by `./gradlew -p tests :ast-benchmark:jmhBenchmark` in [astBenchmarkReportsDir]
 * - the compiler (JMH) benchmarks written by `./gradlew -p tests :compiler-benchmark:jmhBenchmark` in [compilerBenchmarkReportsDir]
 *
 * This script expects:
 *
 * - DD_API_KEY env variable: the Datadog API key. If absent, nothing is uploaded to Datadog.
 * - GITHUB_TOKEN env variable: a token allowed to update the benchmarks issue. If absent, the issue
 * is not updated.
 *
 * This script must be run from the repo root
 */

val ddMetricPrefix = "apollo.kotlin"
val ddDashboardUrl = "https://p.datadoghq.com/sb/d11002689-48ff7001681977d5a09c3a0775632cfa"

/**
 * Where the micro benchmarks results are read from.
 * Must be kept in sync with `scripts/run-firebase-benchmarks.main.kts`
 */
val firebaseResultsFile = File("benchmark/build/firebase-benchmarks.json")

/**
 * Where the native benchmarks results are read from.
 * Must be kept in sync with `tests/native-benchmarks/src/appleTest/kotlin/benchmarks/BenchmarksTest.kt`
 */
val nativeResultsFile = File("tests/native-benchmarks/build/measurements.json")

/**
 * Where the `kotlinx.benchmark` (JMH) reports are written to by `./gradlew :ast-benchmark:jmhBenchmark`
 * and `./gradlew :compiler-benchmark:jmhBenchmark`. Each run creates a new timestamped subdirectory.
 */
val astBenchmarkReportsDir = File("tests/ast-benchmark/build/reports/benchmarks/main")
val compilerBenchmarkReportsDir = File("tests/compiler-benchmark/build/reports/benchmarks/main")

val now = System.currentTimeMillis() / 1000

fun getOptionalEnvVariable(name: String): String? {
  return System.getenv(name)?.ifBlank {
    null
  }
}

data class TestResult(
    val firebaseUrl: String,
    val cases: List<Case>,
    val extraMetrics: List<ExtraMetric>,
)

data class Case(
    val clazz: String,
    val test: String,
    val nanos: Long,
    val allocs: Long,
) {
  val fqName = "${clazz}.$test"
}

data class ExtraMetric(
    val name: String,
    val value: Long,
    val tags: List<String>,
)

data class NativeBenchmark(
    /**
     * The metric name. Native benchmarks use fully qualified names like
     * `apollo.kotlin.native.simplequery.nocache` so [ddMetricPrefix] is not prepended.
     */
    val name: String,
    val measurements: List<Long>,
)

data class JmhBenchmark(
    /**
     * The JMH benchmark name, e.g. `benchmark.ApolloValidationBenchmark.apollo`.
     */
    val name: String,
    val scoreUnit: String,
    val score: Double,
    val scoreError: Double,
    val measurements: List<Double>,
)

fun readJson(file: File, producedBy: String): Any {
  check(file.exists()) {
    "Cannot find '${file.absolutePath}'. Did you run '$producedBy' first?"
  }

  return Json.parseToJsonElement(file.readText()).toAny()!!
}

/**
 * Reads the micro benchmarks results. See `scripts/run-firebase-benchmarks.main.kts` for the writer.
 */
fun readTestResult(): TestResult {
  val map = readJson(firebaseResultsFile, "scripts/run-firebase-benchmarks.main.kts").asMap

  return TestResult(
      firebaseUrl = map["firebaseUrl"].asString,
      cases = map["cases"].asList.map { it.asMap }.map {
        Case(
            clazz = it["clazz"].asString,
            test = it["test"].asString,
            nanos = it["nanos"].asNumber.toLong(),
            allocs = it["allocs"].asNumber.toLong(),
        )
      },
      extraMetrics = map["extraMetrics"].asList.map { it.asMap }.map {
        ExtraMetric(
            name = it["name"].asString,
            value = it["value"].asNumber.toLong(),
            tags = it["tags"].asList.map { tag -> tag.asString },
        )
      },
  )
}

/**
 * Reads the native benchmarks results:
 *
 * ```
 * {
 *   "benchmarks": [
 *     {
 *       "name": "apollo.kotlin.native.simplequery.nocache",
 *       "measurements": [1234, 1235, ...]
 *     },
 *     ...
 *   ]
 * }
 * ```
 */
fun readNativeBenchmarks(): List<NativeBenchmark> {
  val map = readJson(nativeResultsFile, "./gradlew -p tests :native-benchmarks:allTests").asMap

  return map["benchmarks"].asList.map { it.asMap }.map {
    NativeBenchmark(
        name = it["name"].asString,
        measurements = it["measurements"].asList.map { measurement -> measurement.asNumber.toLong() },
    )
  }
}

/**
 * Finds the `jmh.json` report written by the most recent run of the `kotlinx.benchmark` `jmhBenchmark`
 * task in [reportsDir] (each run creates a new timestamped subdirectory).
 */
fun findLatestJmhReportFile(reportsDir: File, producedBy: String): File {
  check(reportsDir.exists()) {
    "Cannot find '${reportsDir.absolutePath}'. Did you run '$producedBy' first?"
  }
  val latestRunDir = reportsDir.listFiles { file -> file.isDirectory }?.maxByOrNull { it.name }
  checkNotNull(latestRunDir) {
    "Cannot find any report in '${reportsDir.absolutePath}'. Did you run '$producedBy' first?"
  }
  return File(latestRunDir, "jmh.json")
}

/**
 * Reads a `kotlinx.benchmark` (JMH) JSON report from [reportsDir].
 */
fun readJmhBenchmarks(reportsDir: File, producedBy: String): List<JmhBenchmark> {
  val file = findLatestJmhReportFile(reportsDir, producedBy)

  return readJson(file, producedBy).asList.map { it.asMap }.map {
    val primaryMetric = it["primaryMetric"].asMap
    JmhBenchmark(
        name = it["benchmark"].asString,
        scoreUnit = primaryMetric["scoreUnit"].asString,
        score = primaryMetric["score"].asNumber.toDouble(),
        scoreError = primaryMetric["scoreError"].asNumber.toDouble(),
        measurements = primaryMetric["rawData"].asList.flatMap { fork -> fork.asList.map { measurement -> measurement.asNumber.toDouble() } },
    )
  }
}

fun formattedTestResult(title: String, testResult: TestResult): String {
  return buildString {
    appendLine("## $title")
    appendLine("### Last Run: ${Date()}")
    appendLine("* Firebase console: [link](${testResult.firebaseUrl})")
    appendLine("* Datadog dashboard: [link](${ddDashboardUrl})")
    appendLine()
    appendLine("<details>")
    appendLine("<summary>Test Cases</summary>")
    appendLine()
    appendLine("| Test Case | Nanos | Allocs |")
    appendLine("|-----------|-------|--------|")
    testResult.cases.forEach {
      appendLine("|${it.fqName}|${it.nanos}|${it.allocs}|")
    }
    appendLine()
    appendLine("</details>")
  }
}

fun formattedNativeBenchmarks(title: String, nativeBenchmarks: List<NativeBenchmark>): String {
  return buildString {
    appendLine("## $title")
    appendLine("### Last Run: ${Date()}")
    appendLine("* Datadog dashboard: [link](${ddDashboardUrl})")
    appendLine()
    appendLine("<details>")
    appendLine("<summary>Test Cases</summary>")
    appendLine()
    appendLine("| Test Case | Measurements (ns) |")
    appendLine("|-----------|--------------------|")
    nativeBenchmarks.forEach {
      appendLine("|${it.name}|${it.measurements.joinToString(", ")}|")
    }
    appendLine()
    appendLine("</details>")
  }
}

fun formattedJmhBenchmarks(title: String, benchmarks: List<JmhBenchmark>): String {
  return buildString {
    appendLine("## $title")
    appendLine("### Last Run: ${Date()}")
    appendLine("* Datadog dashboard: [link](${ddDashboardUrl})")
    appendLine()
    appendLine("<details>")
    appendLine("<summary>Test Cases</summary>")
    appendLine()
    appendLine("| Test Case | Score | Error | Unit |")
    appendLine("|-----------|-------|-------|------|")
    benchmarks.forEach {
      appendLine("|${it.name}|${"%.3f".format(it.score)}|± ${"%.3f".format(it.scoreError)}|${it.scoreUnit}|")
    }
    appendLine()
    appendLine("</details>")
  }
}

val BENCHMARKS_DAHSBOARD_ISSUE_ID = "I_kwDOBCQEc85Mw07L"
fun updateGithubIssue(
    testResult: TestResult,
    nativeBenchmarks: List<NativeBenchmark>,
    astBenchmarks: List<JmhBenchmark>,
    compilerBenchmarks: List<JmhBenchmark>,
    githubToken: String,
) {
  val body = formattedTestResult("Micro benchmarks", testResult) + "\n" +
      formattedNativeBenchmarks("Native benchmarks", nativeBenchmarks) + "\n" +
      formattedJmhBenchmarks("AST benchmarks", astBenchmarks) + "\n" +
      formattedJmhBenchmarks("Compiler benchmarks", compilerBenchmarks)

  val mutation = """
    mutation updateIssue(${'$'}id: ID!, ${'$'}body: String!) {
      updateIssue(input: {id: ${'$'}id, body: ${'$'}body} ){
        clientMutationId
      }
    }
    """.trimIndent()
  val variables = mapOf(
      "id" to BENCHMARKS_DAHSBOARD_ISSUE_ID,
      "body" to body
  )
  println("updating issue $BENCHMARKS_DAHSBOARD_ISSUE_ID....")

  ghGraphQL(mutation, githubToken, variables)
}

fun ghGraphQL(operation: String, ghToken: String, variables: Map<String, String> = emptyMap()): Map<String, Any?> {
  val headers = mapOf("Authorization" to "bearer $ghToken")
  val response = graphQL(
      url = "https://api.github.com/graphql",
      operation = operation,
      headers = headers,
      variables = variables
  )

  val data = response.get("data")

  return data.asMap
}

/**
 * A Datadog serie. [metric] is used as-is, see the [PrefixedSerie] overloads to prepend [ddMetricPrefix].
 */
fun Serie(metric: String, values: List<Number>, tags: List<String>): Map<String, Any> {
  return mapOf(
      "metric" to metric,
      "type" to 0,
      "points" to values.map {
        mapOf(
            "timestamp" to now,
            "value" to it
        )
      },
      "tags" to tags
  )
}

fun PrefixedSerie(name: String, value: Long, tags: List<String>): Map<String, Any> {
  return Serie("$ddMetricPrefix.$name", listOf(value), tags)
}

fun PrefixedSerie(clazz: String, test: String, name: String, value: Long): Map<String, Any> {
  return PrefixedSerie(
      name,
      value,
      listOf(
          "class:$clazz",
          "test:$test"
      )
  )
}

fun TestResult.toSeries(): List<Map<String, Any>> {
  return cases.flatMap {
    listOf(
        PrefixedSerie(it.clazz, it.test, "nanos", it.nanos),
        PrefixedSerie(it.clazz, it.test, "allocs", it.allocs)
    )
  } + extraMetrics.map {
    PrefixedSerie(it.name, it.value, it.tags)
  }
}

fun List<NativeBenchmark>.toSeries(): List<Map<String, Any>> {
  return map {
    Serie(it.name, it.measurements, emptyList())
  }
}

/**
 * [module] is prepended to the metric name since, unlike native benchmarks, JMH benchmark names
 * (e.g. `benchmark.ApolloValidationBenchmark.apollo`) are not already fully qualified.
 */
fun List<JmhBenchmark>.toSeries(module: String): List<Map<String, Any>> {
  return map {
    Serie("$ddMetricPrefix.jmh.$module.${it.name}", it.measurements, listOf("unit:${it.scoreUnit}"))
  }
}

fun uploadToDatadog(datadogApiKey: String, series: List<Map<String, Any>>) {
  val body = mapOf("series" to series)

  val response = body.toJsonElement().toString().let {
    Request.Builder().url("https://api.datadoghq.com/api/v2/series")
        .post(it.toRequestBody("application/json".toMediaType()))
        .addHeader("DD-API-KEY", datadogApiKey)
        .build()
  }.let {
    OkHttpClient.Builder()
        .build()
        .newCall(it)
        .execute()
  }

  check(response.isSuccessful) {
    "Cannot post to Datadog: '${response.code}'\n${response.body?.string()}"
  }
  println("posted ${series.size} series to Datadog")
}

fun main() {
  val testResult = readTestResult()
  val nativeBenchmarks = readNativeBenchmarks()
  val astBenchmarks = readJmhBenchmarks(astBenchmarkReportsDir, "./gradlew -p tests :ast-benchmark:jmhBenchmark")
  val compilerBenchmarks = readJmhBenchmarks(compilerBenchmarkReportsDir, "./gradlew -p tests :compiler-benchmark:jmhBenchmark")

  val datadogApiKey = getOptionalEnvVariable("DD_API_KEY")
  if (datadogApiKey != null) {
    uploadToDatadog(
        datadogApiKey,
        testResult.toSeries() +
            nativeBenchmarks.toSeries() +
            astBenchmarks.toSeries("ast-benchmark") +
            compilerBenchmarks.toSeries("compiler-benchmark")
    )
  }

  val githubToken = getOptionalEnvVariable("GITHUB_TOKEN")
  if (githubToken != null) {
    updateGithubIssue(testResult, nativeBenchmarks, astBenchmarks, compilerBenchmarks, githubToken)
  }
}


main()
