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
 * Reads the micro benchmarks results written by `scripts/run-benchmarks.main.kts`, uploads them to
 * Datadog and updates the benchmarks dashboard issue.
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
 * Where the results are read from. Must be kept in sync with `scripts/run-benchmarks.main.kts`
 */
val resultsFile = File("benchmark/build/benchmarks.json")

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

fun readTestResult(file: File): TestResult {
  check(file.exists()) {
    "Cannot find '${file.absolutePath}'. Did you run 'scripts/run-benchmarks.main.kts' first?"
  }

  val map = Json.parseToJsonElement(file.readText()).toAny().asMap

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

val BENCHMARKS_DAHSBOARD_ISSUE_ID = "I_kwDOBCQEc85Mw07L"
fun updateGithubIssue(testResult: TestResult, githubToken: String) {
  val body = formattedTestResult("Micro benchmarks", testResult)

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

fun uploadToDatadog(datadogApiKey: String, cases: List<Case>, extraMetrics: List<ExtraMetric>) {
  val body = mapOf(
      "series" to cases.flatMap {
        listOf(
            Serie(it.clazz, it.test, "nanos", it.nanos, now),
            Serie(it.clazz, it.test, "allocs", it.allocs, now)
        )
      } + extraMetrics.map {
        Serie(it.name, it.value, it.tags, now)
      }
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

fun main() {
  val testResult = readTestResult(resultsFile)

  val datadogApiKey = getOptionalEnvVariable("DD_API_KEY")
  if (datadogApiKey != null) {
    uploadToDatadog(datadogApiKey, testResult.cases, testResult.extraMetrics)
  }

  val githubToken = getOptionalEnvVariable("GITHUB_TOKEN")
  if (githubToken != null) {
    updateGithubIssue(testResult, githubToken)
  }
}


main()
