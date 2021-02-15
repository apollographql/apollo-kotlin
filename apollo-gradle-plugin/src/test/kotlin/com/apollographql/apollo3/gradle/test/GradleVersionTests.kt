package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.internal.DefaultApolloExtension.Companion.MIN_GRADLE_VERSION
import com.apollographql.apollo.gradle.util.TestUtils
import com.google.common.truth.Truth
import junit.framework.Assert.fail
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Test

class GradleVersionTests  {
  @Test
  fun `minGradleVersion is working and does not show warnings`() {
    TestUtils.withSimpleProject { dir ->
      val result = TestUtils.executeGradleWithVersion(dir, MIN_GRADLE_VERSION,"generateApolloSources")

      Truth.assertThat(result.task(":generateApolloSources")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
      Truth.assertThat(result.output).doesNotContain("Deprecated Gradle features were used in this build")
    }
  }

  @Test
  fun `gradle below minGradleVersion shows an error`() {
    TestUtils.withSimpleProject { dir ->
      try {
        TestUtils.executeGradleWithVersion(dir, "5.4","generateApolloSources")
        fail("Compiling with an old version of Gradle should fail")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("apollo-android requires Gradle version $MIN_GRADLE_VERSION or greater")
      }
    }
  }
}