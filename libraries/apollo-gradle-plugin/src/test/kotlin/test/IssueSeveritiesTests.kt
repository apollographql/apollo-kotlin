package test

import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Test
import util.TestUtils
import util.TestUtils.withTestProject
import kotlin.test.assertTrue

class IssueSeveritiesTests {

  private fun String.containsWarning(): Boolean {
    return lines().any {
      it.matches(Regex(".*w:.* Fragment 'queryDetails' is not used.*"))
    }
  }

  private fun String.containsError(): Boolean {
    return lines().any {
      it.matches(Regex(".*e:.* Fragment 'queryDetails' is not used.*"))
    }
  }

  @Test
  fun default() {
    withTestProject("issue-severities") { dir ->
      val result = TestUtils.executeTask("generateDefaultApolloSources", dir)
      assertTrue(result.output.containsWarning())
    }
  }

  @Test
  fun ignore() {
    withTestProject("issue-severities") { dir ->
      val result = TestUtils.executeTask("generateIgnoreApolloSources", dir)
      assertTrue(!result.output.containsWarning() && !result.output.containsError())
    }
  }

  @Test
  fun warn() {
    withTestProject("issue-severities") { dir ->
      val result = TestUtils.executeTask("generateWarnApolloSources", dir)
      assertTrue(result.output.containsWarning())
    }
  }

  @Test
  fun error() {
    withTestProject("issue-severities") { dir ->
      try {
        TestUtils.executeTask("generateErrorApolloSources", dir)
        error("An Exception was expected")
      } catch (e: UnexpectedBuildFailure) {
        assertTrue(e.message!!.containsError())
      }
    }
  }

  @Test
  fun invalid() {
    withTestProject("issue-severities") { dir ->
      try {
        TestUtils.executeTask("generateInvalidApolloSources", dir)
        error("An Exception was expected")
      } catch (e: UnexpectedBuildFailure) {
        assertTrue(e.message!!.contains("Unknown severity 'invalid'. Expected one of: 'ignore', 'warn', 'error'"))
      }
    }
  }
}
