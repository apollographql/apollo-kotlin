package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.gradle.internal.DefaultApolloExtension.Companion.MIN_GRADLE_VERSION
import com.apollographql.apollo3.gradle.util.TestUtils
import com.apollographql.apollo3.gradle.util.TestUtils.withTestProject
import com.google.common.truth.Truth
import junit.framework.Assert.fail
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Test

class GradleVersionTests  {
  @Test
  fun `minGradleVersion is working and does not show warnings`() {
    withTestProject("gradle-min-version") { dir ->
      val result = TestUtils.executeGradleWithVersion(dir, MIN_GRADLE_VERSION,"generateApolloSources")

      Truth.assertThat(result.task(":generateApolloSources")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
      Truth.assertThat(result.output).doesNotContain("Deprecated Gradle features were used in this build")
    }
  }

  @Test
  fun `gradle below minGradleVersion shows an error`() {
    withTestProject("gradle-min-version") { dir ->
      try {
        TestUtils.executeGradleWithVersion(dir, "5.4","generateApolloSources")
        fail("Compiling with an old version of Gradle should fail")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("apollo-android requires Gradle version $MIN_GRADLE_VERSION or greater")
      }
    }
  }
}