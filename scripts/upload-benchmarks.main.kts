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
import java.lang.management.ManagementFactory
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
    val `class`: String,
    val test: String,
    val nanos: Double,
)

data class JmhBenchmark(
    val `class`: String,
    val test: String,
    val score: Double,
)

data class MachineInfo(
    val os: String,
    val cores: Int,
    val totalMemoryBytes: Long,
)

/**
 * Reads information about the machine running this script, to help interpret benchmark results
 * that may vary between different CI runners.
 */
fun readMachineInfo(): MachineInfo {
  val osMxBean = ManagementFactory.getOperatingSystemMXBean()
  // Reflection is used against the public `com.sun.management.OperatingSystemMXBean` interface (rather
  // than casting osMxBean and calling the method directly) because the method was renamed between JDK
  // versions, and against the interface (rather than osMxBean's own hidden implementation class) because
  // that implementation class lives in a module that doesn't export it.
  val osMxBeanClass = Class.forName("com.sun.management.OperatingSystemMXBean")
  val totalMemoryBytes = try {
    osMxBeanClass.getMethod("getTotalMemorySize").invoke(osMxBean) as Long
  } catch (_: NoSuchMethodException) {
    osMxBeanClass.getMethod("getTotalPhysicalMemorySize").invoke(osMxBean) as Long
  }

  return MachineInfo(
      os = "${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")})",
      cores = Runtime.getRuntime().availableProcessors(),
      totalMemoryBytes = totalMemoryBytes,
  )
}

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
 * ```json
 * {
 *   "benchmarks": [
 *     {
 *       "class": "okio.RealBufferedSink",
 *       "test": "benchmarkSimpleQuery",
 *       "nanos": 3.429887793E8
 *     },
 *     {
 *       "class": "okio.RealBufferedSink",
 *       "test": "benchmarkSimpleQueryWithMemoryCache",
 *       "nanos": 6.2430175E7
 *     }
 *   ]
 * }
 * ```
 */
fun readNativeBenchmarks(): List<NativeBenchmark> {
  val map = readJson(nativeResultsFile, "./gradlew -p tests :native-benchmarks:allTests").asMap

  return map["benchmarks"].asList.map { it.asMap }.map {
    NativeBenchmark(
        `class` = it["class"].asString,
        test = it["test"].asString,
        nanos = it["nanos"].asNumber.toDouble(),
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
        `class` = it["benchmark"].asString.substringBeforeLast('.'),
        test = it["benchmark"].asString.substringAfterLast('.'),
        score = primaryMetric["score"].asNumber.toDouble(),
    )
  }
}

/**
 * The information shared by every kind of benchmark: when they ran, where to find them in Datadog
 * and Firebase, and the machine that produced them.
 */
fun formattedHeader(testResult: TestResult, machineInfo: MachineInfo): String {
  val ramGb = machineInfo.totalMemoryBytes / 1024.0 / 1024.0 / 1024.0
  return buildString {
    appendLine("### Last Run: ${Date()}")
    appendLine("* Datadog dashboard: [link](${ddDashboardUrl})")
    appendLine("* Firebase console: [link](${testResult.firebaseUrl})")
    appendLine("* Machine: ${machineInfo.os}, ${machineInfo.cores} cores, ${"%.1f".format(ramGb)} GB RAM")
  }
}

fun formattedTestResult(title: String, testResult: TestResult): String {
  return buildString {
    appendLine("## $title")
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
    appendLine()
    appendLine("<details>")
    appendLine("<summary>Test Cases</summary>")
    appendLine()
    appendLine("| Test Case | nanos |")
    appendLine("|-----------|--------------------|")
    nativeBenchmarks.forEach {
      appendLine("|${it.`class`}.${it.test}|${it.nanos}|")
    }
    appendLine()
    appendLine("</details>")
  }
}

fun formattedJmhBenchmarks(title: String, benchmarks: List<JmhBenchmark>): String {
  return buildString {
    appendLine("## $title")
    appendLine()
    appendLine("<details>")
    appendLine("<summary>Test Cases</summary>")
    appendLine()
    appendLine("| Test Case | Ops |")
    appendLine("|-----------|-------|")
    benchmarks.forEach {
      appendLine("|${it.`class`}.${it.test}|${it.score}|")
    }
    appendLine()
    appendLine("</details>")
  }
}

// The id of https://github.com/apollographql/apollo-kotlin/issues/4231
val BENCHMARKS_DAHSBOARD_ISSUE_ID = "I_kwDOBCQEc85Mw07L"

fun updateGithubIssue(
    testResult: TestResult,
    nativeBenchmarks: List<NativeBenchmark>,
    astBenchmarks: List<JmhBenchmark>,
    compilerBenchmarks: List<JmhBenchmark>,
    machineInfo: MachineInfo,
    githubToken: String,
) {
  val body = formattedHeader(testResult, machineInfo) + "\n" +
      formattedTestResult("Firebase benchmarks", testResult) + "\n" +
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
 * The only metric names benchmarks are allowed to report to Datadog. Everything else that
 * distinguishes one measurement from another (class, test, module, ...) must be expressed as a tag
 * instead, so that all benchmarks of a kind live under the same metric in the dashboard.
 */
val ddMetricNames = setOf("nanos", "allocs", "bytes", "ops")

/**
 * A Datadog serie for `$ddMetricPrefix.$metric`. [metric] must be one of [ddMetricNames].
 */
fun Serie(metric: String, values: List<Number>, tags: List<String>): Map<String, Any> {
  require(metric in ddMetricNames) {
    "Unknown metric '$metric', must be one of $ddMetricNames"
  }
  return mapOf(
      "metric" to "$ddMetricPrefix.$metric",
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

private fun tags(clazz: String, test: String): List<String> {
  return listOf(
      "class:$clazz",
      "test:$test",
  )
}

fun TestResult.toSeries(): List<Map<String, Any>> {
  return cases.flatMap {
    val tags = tags(it.clazz, it.test)
    listOf(
        Serie("nanos", listOf(it.nanos), tags),
        Serie("allocs", listOf(it.allocs), tags),
    )
  } + extraMetrics.map {
    Serie(it.name, listOf(it.value), it.tags)
  }
}

@JvmName("nativeToSeries")
fun List<NativeBenchmark>.toSeries(): List<Map<String, Any>> {
  return map {
    Serie("nanos", listOf(it.nanos), tags(it.`class`, it.test))
  }
}

fun List<JmhBenchmark>.toSeries(): List<Map<String, Any>> {
  return map {
    Serie("ops", listOf(it.score), tags(it.`class`, it.test))
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
            astBenchmarks.toSeries() +
            compilerBenchmarks.toSeries()
    )
  }

  val githubToken = getOptionalEnvVariable("GITHUB_TOKEN")
  if (githubToken != null) {
    updateGithubIssue(testResult, nativeBenchmarks, astBenchmarks, compilerBenchmarks, readMachineInfo(), githubToken)
  }
}


main()
