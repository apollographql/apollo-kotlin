package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.internal.ApolloPlugin.Companion.MIN_GRADLE_VERSION
import com.apollographql.apollo.gradle.internal.child
import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.generatedChild
import com.google.common.truth.Truth
import junit.framework.Assert.fail
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.junit.Assert
import org.junit.Test
import java.nio.file.Files

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
        TestUtils.executeGradleWithVersion(dir, "6.0","generateApolloSources")
        fail("Compiling with an old version ofo Gradle should fail")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("apollo-android requires Gradle version $MIN_GRADLE_VERSION or greater")
      }
    }
  }
}