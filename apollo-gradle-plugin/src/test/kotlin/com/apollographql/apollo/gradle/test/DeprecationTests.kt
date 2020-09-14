package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.util.TestUtils
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
      val result = TestUtils.executeTask("generateMainServiceApolloSources", dir)
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateMainServiceApolloSources")!!.outcome)
      Truth.assertThat(result.output).contains("operations.graphql:6:7: ApolloGraphQL: Use of deprecated field 'number'")
      Truth.assertThat(result.output).contains("operations.graphql:3:5: ApolloGraphQL: Use of deprecated field 'name'")
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
      val result = TestUtils.executeTask("generateMainServiceApolloSources", dir)
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateMainServiceApolloSources")!!.outcome)
      Truth.assertThat(result.output).doesNotContain("ApolloGraphQL: Use of deprecated field")
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
        TestUtils.executeTask("generateMainServiceApolloSources", dir)
        fail("generateMainServiceApolloSources was expected to fail")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("Warnings found and 'failOnWarnings' is true, aborting")
      }
    }
  }
}