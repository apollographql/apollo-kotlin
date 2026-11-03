package test

import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Test
import util.TestUtils
import util.TestUtils.withTestProject
import util.generatedSource
import util.replaceInText
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

  @Test
  fun `multi-module severity and failOnWarnings apply before source writes`() {
    testProjectWithIsolatedProjectsWorkaround("multi-modules") { dir ->
      dir.resolve("root/src/main/graphql/com/library/operations.graphql").appendText("""

        fragment queryDetails on Query { __typename }
      """.trimIndent())
      val buildFile = dir.resolve("root/build.gradle.kts")
      buildFile.replaceInText("mapScalar(", "failOnWarnings.set(true)\n    mapScalar(")
      try {
        TestUtils.executeTask(":root:generateServiceApolloSources", dir)
        error("An Exception was expected")
      } catch (e: UnexpectedBuildFailure) {
        assertTrue(e.message!!.contains("Execution failed for task ':root:generateServiceApolloSources'"))
        assertTrue(e.message!!.containsWarning())
        assertTrue(e.message!!.contains("Warnings found and 'failOnWarnings' is true"))
        assertTrue(!dir.resolve("root").generatedSource("com/library/fragment/QueryDetails.kt").exists())
      }

      buildFile.replaceInText("failOnWarnings.set(true)", """issueSeverity("UnusedFragment", "error")""")
      try {
        TestUtils.executeTask(":root:generateServiceApolloSources", dir)
        error("An Exception was expected")
      } catch (e: UnexpectedBuildFailure) {
        assertTrue(e.message!!.containsError())
        assertTrue(!e.message!!.containsWarning())
      }

      buildFile.replaceInText("""issueSeverity("UnusedFragment", "error")""", """issueSeverity("UnusedFragment", "ignore")""")
      val ignored = TestUtils.executeTask(":root:generateServiceApolloSources", dir)
      assertTrue(!ignored.output.containsWarning() && !ignored.output.containsError())
      assertTrue(dir.resolve("root").generatedSource("com/library/fragment/QueryDetails.kt").isFile)
    }
  }
}
