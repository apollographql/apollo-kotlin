package com.apollographql.apollo3.gradle.test.platformapi

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Test
import util.TestUtils
import util.replaceInText
import java.io.File

/**
 * Tests using the Apollo Platform API.
 *
 * These tests are not run by default (they are excluded in the gradle conf) because they rely on an external service.
 *
 * They are enabled only when running from the specific `platform-api-tests` CI workflow.
 */
class PlatformApiTests {
  private fun getKey() = System.getenv("PLATFORM_API_TESTS_KEY")
  private fun getGraph() = System.getenv("PLATFORM_API_TESTS_GRAPH")
  private fun getSubgraph() = System.getenv("PLATFORM_API_TESTS_SUBGRAPH")
  private fun getPQListId() = System.getenv("PLATFORM_API_TESTS_PQ_LIST_ID")

  private fun withPlatformApiProject(block: (File) -> Unit) {
    TestUtils.withTestProject("platform-api") { dir ->
      with(File(dir, "build.gradle.kts")) {
        replaceInText("<key>", getKey())
        replaceInText("<graph>", getGraph())
        replaceInText("<PQListId>", getPQListId())

        block(dir)
      }
    }
  }

  @Test
  fun `registerServiceApolloOperations succeeds`() {
    withPlatformApiProject { dir ->
      val result = TestUtils.executeTask("registerServiceApolloOperations", dir)
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":registerServiceApolloOperations")!!.outcome)
    }
  }

  @Test
  fun `downloadServiceApolloSchemaFromRegistry succeeds`() {
    withPlatformApiProject { dir ->
      val result = TestUtils.executeTask("downloadServiceApolloSchemaFromRegistry", dir)
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":downloadServiceApolloSchemaFromRegistry")!!.outcome)
    }
  }

  @Test
  fun `pushApolloSchema succeeds`() {
    withPlatformApiProject { dir ->
      val result = TestUtils.executeTask(
          task = "pushApolloSchema",
          projectDir = dir,
          "--key=${getKey()}",
          "--subgraph=${getSubgraph()}",
          "--schema=src/main/graphql/schema.graphqls",
          "--revision=1",
          "--graphVariant=main"
      )
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":pushApolloSchema")!!.outcome)
    }
  }
}
