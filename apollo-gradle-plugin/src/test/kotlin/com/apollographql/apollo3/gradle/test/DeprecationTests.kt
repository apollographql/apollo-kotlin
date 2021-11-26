package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.gradle.util.TestUtils
import com.google.common.truth.Truth
import junit.framework.Assert.fail
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Assert
import org.junit.Test
import java.io.File

class DeprecationTests {
  @Test
  fun `deprecation warnings are shown by default`() {
    TestUtils.withTestProject("deprecationWarnings") { dir ->
      val result = TestUtils.executeTask("generateServiceApolloSources", dir)
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateServiceApolloSources")!!.outcome)
      Truth.assertThat(result.output).contains("Apollo: Use of deprecated field `name`")
      Truth.assertThat(result.output).contains("Apollo: Use of deprecated field `number`")
    }
  }

  @Test
  fun `deprecation warnings can be silenced`() {
    TestUtils.withTestProject("deprecationWarnings") { dir ->
      File(dir, "build.gradle.kts").appendText("""
        configure<ApolloExtension> {
          warnOnDeprecatedUsages.set(false)
        }
      """.trimIndent())
      val result = TestUtils.executeTask("generateServiceApolloSources", dir)
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateServiceApolloSources")!!.outcome)
      Truth.assertThat(result.output).doesNotContain("Apollo: Use of deprecated field")
    }
  }

  @Test
  fun `failOnWarnings works as expected`() {
    TestUtils.withTestProject("deprecationWarnings") { dir ->
      File(dir, "build.gradle.kts").appendText("""
        configure<ApolloExtension> {
          failOnWarnings.set(true)
        }
      """.trimIndent())
      try {
        TestUtils.executeTask("generateServiceApolloSources", dir)
        fail("generateServiceApolloSources was expected to fail")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("Warnings found and 'failOnWarnings' is true, aborting")
      }
    }
  }
}