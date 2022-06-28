#!/usr/bin/env kotlin

@file:DependsOn("com.squareup.okio:okio:3.2.0")
@file:DependsOn("com.google.cloud:google-cloud-storage:2.8.1")
@file:DependsOn("net.mbonnin.bare-graphql:bare-graphql:0.0.2")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.mbonnin.bare.graphql.asList
import net.mbonnin.bare.graphql.asMap
import net.mbonnin.bare.graphql.cast
import net.mbonnin.bare.graphql.graphQL
import okio.Buffer
import okio.buffer
import okio.source
import java.io.File
import java.util.Date


/**
 * This script expects:
 *
 * - `gcloud` in the path
 * - A Google Cloud Project with "Google Cloud Testing API" and "Cloud Tool Results API" enabled
 * - GOOGLE_SERVICES_JSON env variable: a service account json with Owner permissions (could certainly be less but haven't found a way yet)
 * - GITHUB_REPOSITORY env variable: the repository to create the issue into in the form `owner/name`
 * - GITHUB_TOKEN env variable: a GitHub token that can create issues on the repo
 *
 * This script must be run from the repo root
 */

/**
 * Executes the given command and returns stdout as a String
 * Throws if the exit code is not 0
 */
fun executeCommand(vararg command: String): CommandResult {
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
      while(true) {
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
      while(true) {
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
  val googleServicesJson = System.getenv("GOOGLE_SERVICES_JSON") ?: error("GOOGLE_SERVICES_JSON is missing")

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

  return GCloud(storage, jsonElement.jsonObject.get("project_id")?.jsonPrimitive?.content ?: error("Cannot find project_id"))
}

data class GCloud(val storage: Storage, val projectId: String)

fun runTest(projectId: String): String {
  val result = executeCommand(
      "gcloud",
      "--project",
      projectId,
      "firebase",
      "test",
      "android",
      "run",
      "--type",
      "instrumentation",
      "--device",
      "model=redfin,locale=en,orientation=portrait",
      "--test",
      "benchmark/lib/build/outputs/apk/androidTest/release/benchmark-release-androidTest.apk",
      "--app",
      "benchmark/app/build/outputs/apk/release/app-release.apk"
  )

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
    val matchResult = Regex(".*\\[https://console.developers.google.com/storage/browser/([^\\]]*).*").matchEntire(it)
    matchResult?.groupValues?.get(1)
  }.single()
      .split("/")
      .filter { it.isNotBlank() }
  val bucket = gsUrl[0]
  val blobName = "${gsUrl[1]}/redfin-30-en-portrait/instrumentation.results"

  println("Downloading $bucket/$blobName")
  val buffer = Buffer()
  storage.get(bucket, blobName).downloadTo(buffer.outputStream())

  val cases = buffer.readUtf8().lines().mapNotNull {
    val matchResult = Regex(".*android.studio.display.benchmark= *([0-9,]*) *ns *([0-9]*) *allocs *([^ ]*)").matchEntire(it)
    matchResult?.groupValues
  }.map {
    Case(it.get(3), it.get(1).replace(",", "").toLong(), it.get(2).toLong())
  }

  val firebaseUrl = output.lines().mapNotNull {
    val matchResult = Regex("Test results will be streamed to \\[(.*)\\].").matchEntire(it)
    matchResult?.groupValues?.get(1)
  }.single()

  return TestResult(firebaseUrl, cases)
}

data class TestResult(
    val firebaseUrl: String,
    val cases: List<Case>,
)

data class Case(
    val name: String,
    val nanos: Long,
    val allocs: Long,
)

fun issueBody(testResult: TestResult): String {
  return buildString {
    appendLine("${Date()}")
    appendLine("[${testResult.firebaseUrl}](${testResult.firebaseUrl})")
    appendLine()
    appendLine("Test Cases:")
    appendLine("| Test Case | Nanos | Allocs |")
    appendLine("|-----------|-------|--------|")
    testResult.cases.forEach {
      appendLine("|${it.name}|${it.nanos}|${it.allocs}|")
    }
  }
}

val issueTitle = "Benchmarks dashboard"
fun updateOrCreateGithubIssue(testResult: TestResult) {
  val ghRepo = System.getenv("GITHUB_REPOSITORY") ?: error("GITHUB_REPOSITORY env variable is missing")
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


  val response = ghGraphQL(query)
  val existingIssues = response.get("search").asMap.get("edges").asList

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
        "body" to issueBody(testResult),
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
    variables = mapOf("id" to existingIssues.first().asMap["node"].asMap["id"].cast<String>(), "body" to issueBody(testResult))
    println("updating issue")
  }
  ghGraphQL(mutation, variables)
}

fun ghGraphQL(operation: String, variables: Map<String, String> = emptyMap()): Map<String, Any?> {
  val ghToken = System.getenv("GITHUB_TOKEN") ?: error("GITHUB_TOKEN env variable is missing")
  val headers = mapOf("Authorization" to "bearer $ghToken")
  return graphQL(
      url = "https://api.github.com/graphql",
      operation = operation,
      headers = headers,
      variables = variables
  ).also { println(it) }.get("data").asMap
}

fun main() {
  val gcloud = authenticate()
  val testOutput = runTest(gcloud.projectId)
  val testResult = getTestResult(testOutput, gcloud.storage)
  updateOrCreateGithubIssue(testResult)
  println(testResult)
}

main()