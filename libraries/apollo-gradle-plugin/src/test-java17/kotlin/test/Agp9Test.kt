package test

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertTrue
import org.junit.Test
import util.TestUtils
import util.TestUtils.executeGradleWithVersion
import util.TestUtils.withTestProject

class Agp9Test {
  @Test
  fun `agp9_compiles`() {
    withTestProject("agp9") { dir ->
      val result = executeGradleWithVersion(dir, "9.1.0", "build")
      assertTrue(result.task(":app:build")!!.outcome == TaskOutcome.SUCCESS)
    }
  }
}