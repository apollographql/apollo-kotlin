package com.apollographql.apollo.gradle.test

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Test
import util.TestUtils
import java.io.File

class GradleBuildCacheTests {

  private fun test(testProject: String, task: String, tasksToCheck: List<String>) {
    val extra = """
      buildCache {
      local {
        directory = "../testProjectBuildCache"
      }
    }
    """.trimIndent()

    val buildCacheDir = File(File(System.getProperty("user.dir")), "build/testProjectBuildCache")
    buildCacheDir.deleteRecursively()

    TestUtils.withTestProject(testProject, "testProject1") { dir ->
      dir.resolve("settings.gradle.kts").appendText(extra)

      val result = TestUtils.executeTask(task, dir, "--build-cache")
      (tasksToCheck + task).forEach {
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":$it")!!.outcome)
      }
    }
    TestUtils.withTestProject(testProject, "testProject2") { dir ->
      dir.resolve("settings.gradle.kts").appendText(extra)

      val result = TestUtils.executeTask(task, dir, "--build-cache")
      (tasksToCheck + task).forEach {
        Assert.assertEquals("task $it has wrong outcome", TaskOutcome.FROM_CACHE, result.task(":$it")!!.outcome)
      }
    }
  }

  @Test
  fun `generate apollo classes task are cached`() {
    test("buildCache", "generateServiceApolloSources", emptyList())
  }

  @Test
  fun `generate apollo classes task are cached multimodule`() {
    test(
        testProject = "multi-modules",
        task = "leaf:generateServiceApolloSources",
        tasksToCheck = listOf(
            "leaf:generateServiceApolloIrOperations", "root:generateServiceApolloIrOperations", "root:generateServiceApolloOptions"
        )
    )
  }
}
