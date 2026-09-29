package test

import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Test
import util.TestUtils
import util.TestUtils.withTestProject
import util.replaceInText
import java.io.File
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

  private fun String.containsUsedFragmentWarning(): Boolean {
    return lines().any {
      it.matches(Regex(".*Fragment 'usedQueryDetails' is not used.*"))
    }
  }

  private fun String.containsConstantGuardedFragmentWarning(): Boolean {
    return lines().any {
      it.matches(Regex(".*Fragment 'constantSkippedFragment' is not used.*"))
    }
  }

  @Test
  fun `used fragment is never reported regardless of severity`() {
    withTestProject("issue-severities") { dir ->
      // 'usedQueryDetails' is spread by 'GetTypename' in the same module, so it must stay silent for every
      // severity, including 'error' where 'queryDetails' (genuinely unused) makes the build fail.
      assertTrue(!TestUtils.executeTask("generateDefaultApolloSources", dir).output.containsUsedFragmentWarning())
      assertTrue(!TestUtils.executeTask("generateWarnApolloSources", dir).output.containsUsedFragmentWarning())
      try {
        TestUtils.executeTask("generateErrorApolloSources", dir)
        error("An Exception was expected")
      } catch (e: UnexpectedBuildFailure) {
        assertTrue(!e.message!!.containsUsedFragmentWarning())
      }
    }
  }

  @Test
  fun `fragment spread only behind a constant-false condition is never reported regardless of severity`() {
    withTestProject("issue-severities") { dir ->
      // 'constantSkippedFragment' is spread only behind @skip(if: true), so it still counts as used.
      // It must stay silent for every severity, including 'error'.
      assertTrue(!TestUtils.executeTask("generateDefaultApolloSources", dir).output.containsConstantGuardedFragmentWarning())
      assertTrue(!TestUtils.executeTask("generateWarnApolloSources", dir).output.containsConstantGuardedFragmentWarning())
      try {
        TestUtils.executeTask("generateErrorApolloSources", dir)
        error("An Exception was expected")
      } catch (e: UnexpectedBuildFailure) {
        assertTrue(!e.message!!.containsConstantGuardedFragmentWarning())
      }
    }
  }

  @Test
  fun `succeeds with failOnWarnings once the only genuinely unused fragment is removed`() {
    withTestProject("issue-severities") { dir ->
      // Remove 'queryDetails', the only dead fragment, then confirm failOnWarnings still succeeds.
      File(dir, "src/main/graphql/operation.graphql").replaceInText(
          "\n\nfragment queryDetails on Query {\n    __typename\n}",
          ""
      )
      File(dir, "build.gradle.kts").replaceInText(
          "packageName.set(\"default\")",
          "packageName.set(\"default\")\n    failOnWarnings.set(true)"
      )

      TestUtils.executeTaskAndAssertSuccess(":generateDefaultApolloSources", dir)
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
