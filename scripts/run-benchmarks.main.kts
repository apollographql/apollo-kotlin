#!/usr/bin/env kotlin

@file:DependsOn("com.squareup.okio:okio:3.2.0")
@file:DependsOn("com.google.cloud:google-cloud-storage:2.8.1")
@file:DependsOn("net.mbonnin.bare-graphql:bare-graphql:0.0.2")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.6.2")

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.mbonnin.bare.graphql.asList
import net.mbonnin.bare.graphql.asMap
import net.mbonnin.bare.graphql.asNumber
import net.mbonnin.bare.graphql.asString
import net.mbonnin.bare.graphql.toAny
import net.mbonnin.bare.graphql.toJsonElement
import okio.Buffer
import okio.buffer
import okio.source
import java.io.File
import kotlin.math.roundToLong

/**
 * Runs the micro benchmarks on Firebase Test Lab and writes the results to [resultsFile].
 *
 * Use `scripts/upload-benchmarks.main.kts` to upload those results to Datadog and update the
 * benchmarks dashboard issue.
 *
 * This script expects:
 *
 * - `gcloud` in the path
 * - A Google Cloud Project with "Google Cloud Testing API" and "Cloud Tool Results API" enabled
 * - GOOGLE_SERVICES_JSON env variable: the service account key used to authenticate
 *
 * This script must be run from the repo root
 */

val appApk = "benchmark/app/build/outputs/apk/release/app-release.apk"
val testApk = "benchmark/microbenchmark/build/outputs/apk/androidTest/release/microbenchmark-release-androidTest.apk"
val deviceModel = "redfin,locale=en,orientation=portrait"
val directoriesToPull = "/sdcard/Download"
val environmentVariables = "clearPackageData=true,additionalTestOutputDir=/sdcard/Download,no-isolated-storage=true"

/**
 * Where the results are written. Must be kept in sync with `scripts/upload-benchmarks.main.kts`
 */
val resultsFile = File("benchmark/build/benchmarks.json")

fun getRequiredEnvVariable(name: String): String {
  return getOptionalEnvVariable(name) ?: error("Cannot find env '$name'")
}

fun getOptionalEnvVariable(name: String): String? {
  return System.getenv(name)?.ifBlank {
    null
  }
}

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
  var extraMetrics: List<ExtraMetric>? = null
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

fun locateExtraMetrics(storage: Storage, bucket: String, prefix: String): List<ExtraMetric>? {
  val candidates = storage.list(bucket, Storage.BlobListOption.prefix(prefix)).values
  return candidates.singleOrNull {
    it.name.endsWith("extraMetrics.json")
  }?.let {
    downloadBlob(storage, bucket, it.name)
  }?.let {
    Json.parseToJsonElement(it).toAny()
  }?.parseExtraMetrics()
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
fun Any.parseExtraMetrics(): List<ExtraMetric> {
  return this.asList.map { it.asMap }.map {
    ExtraMetric(
        name = it["name"].asString,
        value = it["value"].asNumber.toLong(),
        tags = it["tags"] as List<String>? ?: emptyList(),
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
    val extraMetrics: List<ExtraMetric>,
)

data class Case(
    val clazz: String,
    val test: String,
    val nanos: Long,
    val allocs: Long,
)

data class ExtraMetric(
    val name: String,
    val value: Long,
    val tags: List<String>,
)

/**
 * Writes [TestResult] as Json. See `scripts/upload-benchmarks.main.kts` for the reader.
 */
fun TestResult.write(file: File) {
  val map = mapOf(
      "firebaseUrl" to firebaseUrl,
      "cases" to cases.map {
        mapOf(
            "clazz" to it.clazz,
            "test" to it.test,
            "nanos" to it.nanos,
            "allocs" to it.allocs,
        )
      },
      "extraMetrics" to extraMetrics.map {
        mapOf(
            "name" to it.name,
            "value" to it.value,
            "tags" to it.tags,
        )
      },
  )

  file.parentFile?.mkdirs()
  file.writeText(map.toJsonElement().toString())
  println("benchmarks written to ${file.absolutePath}")
}

fun main() {
  val gcloud = authenticate()

  val testOutput = runTest(gcloud.projectId, testApk)
  val testResult = getTestResult(testOutput, gcloud.storage)

  testResult.write(resultsFile)
}


main()
