#!/usr/bin/env kotlin

@file:DependsOn("com.squareup.okio:okio:3.2.0")
@file:DependsOn("com.google.cloud:google-cloud-storage:2.8.1")
@file:DependsOn("net.mbonnin.bare-graphql:bare-graphql:0.0.2")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.6.2")
@file:DependsOn("com.squareup.okhttp3:okhttp:4.10.0")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.mbonnin.bare.graphql.asList
import net.mbonnin.bare.graphql.asMap
import net.mbonnin.bare.graphql.asNumber
import net.mbonnin.bare.graphql.asString
import net.mbonnin.bare.graphql.cast
import net.mbonnin.bare.graphql.graphQL
import net.mbonnin.bare.graphql.toAny
import net.mbonnin.bare.graphql.toJsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import okio.buffer
import okio.source
import java.io.File
import java.util.Date
import kotlin.math.roundToLong

/**
 * This script expects:
 *
 * - `gcloud` in the path
 * - A Google Cloud Project with "Google Cloud Testing API" and "Cloud Tool Results API" enabled
 * - GITHUB_REPOSITORY env variable: the repository to create the issue into in the form `owner/name`
 *
 * This script must be run from the repo root
 */

val appApk = "benchmark/app/build/outputs/apk/release/app-release.apk"
val testApk = "benchmark/microbenchmark/build/outputs/apk/androidTest/release/microbenchmark-release-androidTest.apk"
val deviceModel = "redfin,locale=en,orientation=portrait"
val directoriesToPull = "/sdcard/Download"
val environmentVariables = "clearPackageData=true,additionalTestOutputDir=/sdcard/Download,no-isolated-storage=true"
val ddMetricPrefix = "apollo.kotlin"
val ddDashboardUrl = "https://p.datadoghq.com/sb/d11002689-48ff7001681977d5a09c3a0775632cfa"

fun getRequiredEnvVariable(name: String): String {
  return getOptionalEnvVariable(name) ?: error("Cannot find env '$name'")
}

fun getOptionalEnvVariable(name: String): String? {
  return System.getenv(name)?.ifBlank {
    null
  }
}

val now = System.currentTimeMillis() / 1000

/**
 * Executes the given command and returns stdout as a String
 * Throws if the exit code is not 0
 */
fun executeCommand(vararg command: String): CommandResult {
  println("execute: ${command.joinToString(" ")}")

  val process = ProcessBuilder()
      .command(*command)
      .redirectInput(ProcessBuilder.Redirect.INHERIT)
      .start()

  /**
   * Read output and error in a thread to not block the process if the output/error
   * doesn't fit in the buffer
   */
  var output: String? = null
  var error: String? = null
  val outputThread = Thread {
    val buffer = process.inputStream.source().buffer()
    output = buildString {
      while (true) {
        val line = buffer.readUtf8Line()
        if (line == null) {
          break
        }
        println("STDOUT: $line")
        appendLine(line)
      }
    }
  }
  outputThread.start()
  val errorThread = Thread {
    val buffer = process.errorStream.source().buffer()
    error = buildString {
      while (true) {
        val line = buffer.readUtf8Line()
        if (line == null) {
          break
        }
        println("STDERR: $line")
        appendLine(line)
      }
    }
  }
  errorThread.start()

  val exitCode = process.waitFor()

  outputThread.join()
  errorThread.join()
  return CommandResult(exitCode, output ?: "", error ?: "")
}

class CommandResult(val code: Int, val stdout: String, val stderr: String)


/**
 * Authenticates the local 'gcloud' and a new [Storage] instance
 * Throws on error
 */
fun authenticate(): GCloud {
  val googleServicesJson = getRequiredEnvVariable("GOOGLE_SERVICES_JSON")

  val tmpFile: File = File.createTempFile("google", "json")
  val credentials: GoogleCredentials
  val storage: Storage
  try {
    tmpFile.writeText(googleServicesJson)
    val result = executeCommand("gcloud", "auth", "activate-service-account", "--key-file=${tmpFile.absoluteFile}")
    if (result.code != 0) {
      error("Cannot authenticate")
    }
    credentials = GoogleCredentials.fromStream(tmpFile.inputStream())
        .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
    storage = StorageOptions.newBuilder().setCredentials(credentials).build().service
  } finally {
    tmpFile.delete()
  }

  val jsonElement = Json.parseToJsonElement(googleServicesJson)

  return GCloud(
      storage,
      jsonElement.jsonObject.get("project_id")?.jsonPrimitive?.content ?: error("Cannot find project_id")
  )
}

data class GCloud(val storage: Storage, val projectId: String)

/**
 * Run the test remotely. To do the same thing locally, run
 *
 * ./gradlew -p benchmark assembleRelease assembleStableReleaseAndroidTest
 * adb install benchmark/microbenchmark/build/outputs/apk/androidTest/stable/release/microbenchmark-stable-release-androidTest.apk
 * adb shell am instrument -w com.apollographql.apollo.benchmark.stable/androidx.benchmark.junit4.AndroidBenchmarkRunner
 *
 * Or just
 *
 * ./gradlew -p benchmark :microbenchmark:connectedIncubatingReleaseAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.apollographql.apollo.benchmark.CacheIncubatingIntegrationTests#concurrentQueriesTestNetworkTransportMemoryThenSql
 * cat 'benchmark/microbenchmark/build/outputs/androidTest-results/connected/release/flavors/incubating/Pixel 6a - 14/testlog/test-results.log'
 */
fun runTest(projectId: String, testApk: String): String {
  val args = mutableListOf(
      "gcloud",
      "-q", // Disable all interactive prompts
      "--project",
      projectId,
      "firebase",
      "test",
      "android",
      "run",
      "--type",
      "instrumentation",
      "--device",
      "model=$deviceModel",
      "--test",
      testApk,
      "--app",
      appApk,
      "--timeout",
      "30m"
  )

  directoriesToPull.let {
    args.add("--directories-to-pull")
    args.add(it)
  }

  environmentVariables.let {
    args.add("--environment-variables")
    args.add(it)
  }

  val result = executeCommand(*args.toTypedArray())

  check(result.code == 0) {
    "Test failed"
  }

  // Most of the interesting output is in stderr
  return result.stderr
}

/**
 * Parses the 'gcloud firebase test android run' output and download the instrumentation
 * results from Google Cloud Storage
 *
 * @return the [TestResult]
 */
fun getTestResult(output: String, storage: Storage): TestResult {
  val gsUrl = output.lines().mapNotNull {
    val matchResult =
        Regex(".*\\[https://console.developers.google.com/storage/browser/([^\\]]*).*").matchEntire(it)
    matchResult?.groupValues?.get(1)
  }.single()
      .split("/")
      .filter { it.isNotBlank() }
  val bucket = gsUrl[0]

  val blobBase = "${gsUrl[1]}/redfin-30-en-portrait"

  val directory = directoriesToPull.split(",").filter { it.isNotBlank() }.singleOrNull()
  var cases: List<Case>? = null
  var extraMetrics: List<Map<String, Any>>? = null
  if (directory != null) {
    // A directory was provided, look inside it to check if we can find the test results
    cases = locateBenchmarkData(storage, bucket, "$blobBase/artifacts$directory")
    extraMetrics = try {
      locateExtraMetrics(storage, bucket, "$blobBase/artifacts$directory")
    } catch (_: Exception) {
      null
    }
  }

  if (cases == null) {
    // Get the cases from the logs
    cases = downloadBlob(storage, bucket, "$blobBase/instrumentation.results").parseCases()
  }

  val firebaseUrl = output.lines().mapNotNull {
    val matchResult = Regex("Test results will be streamed to \\[(.*)\\].").matchEntire(it)
    matchResult?.groupValues?.get(1)
  }.single()

  return TestResult(firebaseUrl, cases, extraMetrics.orEmpty())
}

fun locateBenchmarkData(storage: Storage, bucket: String, prefix: String): List<Case>? {
  val candidates = storage.list(bucket, Storage.BlobListOption.prefix(prefix)).values
  return candidates.singleOrNull {
    it.name.endsWith("benchmarkData.json")
  }?.let {
    downloadBlob(storage, bucket, it.name)
  }?.let {
    Json.parseToJsonElement(it).toAny()
  }?.parseCasesFromBenchmarkData()
}

fun locateExtraMetrics(storage: Storage, bucket: String, prefix: String): List<Map<String, Any>>? {
  val candidates = storage.list(bucket, Storage.BlobListOption.prefix(prefix)).values
  return candidates.singleOrNull {
    it.name.endsWith("extraMetrics.json")
  }?.let {
    downloadBlob(storage, bucket, it.name)
  }?.let {
    Json.parseToJsonElement(it).toAny()
  }?.parseCasesFromExtraMetrics()
}

/**
 * ```
 * {
 *     "context": {
 *         "build": {
 *             "brand": "google",
 *             "device": "redfin",
 *             "fingerprint": "google/redfin/redfin:11/RQ3A.211001.001/7641976:user/release-keys",
 *             "model": "Pixel 5",
 *             "version": {
 *                 "sdk": 30
 *             }
 *         },
 *         "cpuCoreCount": 8,
 *         "cpuLocked": true,
 *         "cpuMaxFreqHz": 2400000000,
 *         "memTotalBytes": 7819997184,
 *         "sustainedPerformanceModeEnabled": false
 *     },
 *     "benchmarks": [
 *         {
 *             "name": "concurrentReadWritesSql",
 *             "params": {},
 *             "className": "com.apollographql.apollo.benchmark.ApolloStoreTests",
 *             "totalRunTimeNs": 35949947123,
 *             "metrics": {
 *                 "timeNs": {
 *                     "minimum": 3.36396648E8,
 *                     "maximum": 4.54433847E8,
 *                     "median": 3.828202985E8,
 *                     "runs": [
 *                         4.54433847E8,
 *                         4.30116918E8,
 *                         ...
 *                     ]
 *                 },
 *                 "allocationCount": {
 *                     "minimum": 585424.0,
 *                     "maximum": 593386.0,
 *                     "median": 589660.0,
 *                     "runs": [
 *                         589660.0,
 *                         585424.0,
 *                         ...,
 *                     ]
 *                 }
 *             },
 *             "sampledMetrics": {},
 *             "warmupIterations": 30,
 *             "repeatIterations": 1,
 *             "thermalThrottleSleepSeconds": 0
 *         },
 *         ...
 *     ]
 * }
 * ```
 */
fun Any.parseCasesFromBenchmarkData(): List<Case> {
  return this.asMap["benchmarks"].asList.map { it.asMap }.map {
    Case(
        test = it["name"].asString,
        clazz = it["className"].asString,
        nanos = it["metrics"].asMap["timeNs"].asMap["median"].asNumber.toLong(),
        allocs = it["metrics"].asMap["allocationCount"].asMap["median"].asNumber.toLong(),
    )
  }
}

/**
 * ```
 * [
 *   {
 *     "name": "bytes",
 *     "value": 2994176,
 *     "tags": [
 *       "class:com.apollographql.apollo.benchmark.CacheTests",
 *       "test:cacheOperationSql"
 *     ]
 *   },
 *   {
 *     "name": "bytes",
 *     "value": 2994176,
 *     "tags": [
 *       "class:com.apollographql.apollo.benchmark.CacheTests",
 *       "test:cacheResponseSql"
 *     ]
 *   }
 * ]
 * ```
 */
fun Any.parseCasesFromExtraMetrics(): List<Map<String, Any>> {
  return this.asList.map { it.asMap }.map {
    Serie(
        name = it["name"].asString,
        value = it["value"].asNumber.toLong(),
        tags = it["tags"] as List<String>? ?: emptyList(),
        now = now,
    )
  }
}

fun downloadBlob(storage: Storage, bucket: String, blobName: String): String {
  val buffer = Buffer()
  storage.get(bucket, blobName).downloadTo(buffer.outputStream())

  return buffer.readUtf8()
}

/**
 * Heuristics based parser until Firebase Test Labs supports downloading the Json
 */
fun String.parseCases(): List<Case> {
  val cases = mutableListOf<Case>()
  var clazz: String? = null
  var test: String? = null
  var nanos: Long? = null
  var allocs: Long? = null

  val clazzRegex = Regex("INSTRUMENTATION_STATUS: class=(.*)")
  val testRegex = Regex("INSTRUMENTATION_STATUS: test=(.*)")
  val nanosRegex = Regex("INSTRUMENTATION_STATUS: time_nanos_median=(.*)")
  val allocsRegex = Regex("INSTRUMENTATION_STATUS: allocation_count_median=(.*)")

  fun maybeOutput() {
    if (clazz != null && test != null && nanos != null && allocs != null) {
      cases.add(Case(clazz!!, test!!, nanos!!, allocs!!))
      clazz = null
      test = null
      nanos = null
      allocs = null
    }
  }
  lines().forEach {
    var result = clazzRegex.matchEntire(it)
    if (result != null) {
      clazz = result.groupValues[1]
      maybeOutput()
      return@forEach
    }
    result = testRegex.matchEntire(it)
    if (result != null) {
      test = result.groupValues[1]
      maybeOutput()
      return@forEach
    }
    result = nanosRegex.matchEntire(it)
    if (result != null) {
      nanos = result.groupValues[1].toDouble().roundToLong()
      maybeOutput()
      return@forEach
    }
    result = allocsRegex.matchEntire(it)
    if (result != null) {
      allocs = result.groupValues[1].toDouble().roundToLong()
      maybeOutput()
      return@forEach
    }
  }

  return cases
}

data class TestResult(
    val firebaseUrl: String,
    val cases: List<Case>,
    val extraMetrics: List<Map<String, Any>>,
)

data class Case(
    val clazz: String,
    val test: String,
    val nanos: Long,
    val allocs: Long,
) {
  val fqName = "${clazz}.$test"
}

fun formattedTestResult(title: String, testResult: TestResult): String {
  return buildString {
    appendLine("## $title")
    appendLine("### Last Run: ${Date()}")
    appendLine("* Firebase console: [link](${testResult.firebaseUrl})")
    appendLine("* Datadog dashboard: [link](${ddDashboardUrl})")
    appendLine()
    appendLine("### Test Cases:")
    appendLine("| Test Case | Nanos | Allocs |")
    appendLine("|-----------|-------|--------|")
    testResult.cases.forEach {
      appendLine("|${it.fqName}|${it.nanos}|${it.allocs}|")
    }
  }
}

val issueTitle = "Benchmarks dashboard"
fun updateOrCreateGithubIssue(testResult: TestResult, githubToken: String) {
  val ghRepo = getRequiredEnvVariable("GITHUB_REPOSITORY")
  val ghRepositoryOwner = ghRepo.split("/")[0]
  val ghRepositoryName = ghRepo.split("/")[1]

  val query = """
{
  search(query: "$issueTitle repo:$ghRepo", type: ISSUE, first: 100) {
    edges {
      node {
        ... on Issue {
          title
          id
        }
      }
    }
  }
  repository(owner: "$ghRepositoryOwner", name: "$ghRepositoryName") {
    id
  }
}
""".trimIndent()


  val response = ghGraphQL(query, githubToken)
  val existingIssues = response.get("search").asMap.get("edges").asList

  val body = formattedTestResult("Micro benchmarks", testResult)
  val mutation: String
  val variables: Map<String, String>
  if (existingIssues.isEmpty()) {
    mutation = """
mutation createIssue(${'$'}repositoryId: ID!, ${'$'}title: String!, ${'$'}body: String!) {
  createIssue(input: {repositoryId: ${'$'}repositoryId, title: ${'$'}title, body: ${'$'}body} ){
    clientMutationId
  }
}
    """.trimIndent()
    variables = mapOf(
        "title" to issueTitle,
        "body" to body,
        "repositoryId" to response.get("repository").asMap["id"].cast<String>()
    )
    println("creating issue")
  } else {
    mutation = """
mutation updateIssue(${'$'}id: ID!, ${'$'}body: String!) {
  updateIssue(input: {id: ${'$'}id, body: ${'$'}body} ){
    clientMutationId
  }
}
    """.trimIndent()
    variables = mapOf(
        "id" to existingIssues.first().asMap["node"].asMap["id"].cast<String>(),
        "body" to body
    )
    println("updating issue")
  }
  ghGraphQL(mutation, githubToken, variables)
}

fun ghGraphQL(operation: String, ghToken: String, variables: Map<String, String> = emptyMap()): Map<String, Any?> {
  val headers = mapOf("Authorization" to "bearer $ghToken")
  return graphQL(
      url = "https://api.github.com/graphql",
      operation = operation,
      headers = headers,
      variables = variables
  ).get("data").asMap
}

fun Serie(name: String, value: Long, tags: List<String>, now: Long): Map<String, Any> {
  return mapOf(
      "metric" to "$ddMetricPrefix.$name",
      "type" to 0,
      "points" to listOf(
          mapOf(
              "timestamp" to now,
              "value" to value
          )
      ),
      "tags" to tags
  )
}

fun Serie(clazz: String, test: String, name: String, value: Long, now: Long): Map<String, Any> {
  return Serie(
      name,
      value,
      listOf(
          "class:$clazz",
          "test:$test"
      ),
      now
  )
}

fun uploadToDatadog(datadogApiKey: String, cases: List<Case>, extraMetrics: List<Map<String, Any>>) {
  val body = mapOf(
      "series" to cases.flatMap {
        listOf(
            Serie(it.clazz, it.test, "nanos", it.nanos, now),
            Serie(it.clazz, it.test, "allocs", it.allocs, now)
        )
      } + extraMetrics
  )

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
  println("posted to Datadog")
}

fun runTest(gcloud: GCloud, testApk: String): TestResult {
  val testOutput = runTest(gcloud.projectId, testApk)
  return getTestResult(testOutput, gcloud.storage)
}

fun main() = runBlocking {
  val gcloud = authenticate()

  val testResultDeferred = async(Dispatchers.Default) {
    runTest(gcloud, testApk)
  }

  val testResult = testResultDeferred.await()

  val githubToken = getOptionalEnvVariable("GITHUB_TOKEN")
  if (githubToken != null) {
    updateOrCreateGithubIssue(testResult, githubToken)
  }
  val datadogApiKey = getOptionalEnvVariable("DD_API_KEY")
  if (datadogApiKey != null) {
    uploadToDatadog(datadogApiKey, testResult.cases, testResult.extraMetrics)
  }
}


main()
