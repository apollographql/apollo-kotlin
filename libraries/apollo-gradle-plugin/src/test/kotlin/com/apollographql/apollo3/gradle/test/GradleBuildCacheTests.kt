package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.gradle.util.TestUtils
import com.apollographql.apollo3.gradle.util.replaceInText
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Test
import java.io.File

class GradleBuildCacheTests {

  @Test
  fun `generate apollo classes task are cached`() {
    val buildCacheDir = File(File(System.getProperty("user.dir")), "build/testProjectBuildCache")
    buildCacheDir.deleteRecursively()
    TestUtils.withTestProject("buildCache", "testProject1") { dir ->
      val result = TestUtils.executeTask("generateServiceApolloSources", dir, "--build-cache")
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateServiceApolloSources")!!.outcome)
    }
    TestUtils.withTestProject("buildCache", "testProject2") { dir ->
      val result = TestUtils.executeTask("generateServiceApolloSources", dir, "--build-cache")
      Assert.assertEquals(TaskOutcome.FROM_CACHE, result.task(":generateServiceApolloSources")!!.outcome)
    }
  }
}
