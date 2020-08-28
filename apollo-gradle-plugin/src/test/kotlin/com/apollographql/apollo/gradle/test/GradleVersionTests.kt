package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.internal.ApolloPlugin.Companion.MIN_GRADLE_VERSION
import com.apollographql.apollo.gradle.internal.child
import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.generatedChild
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
  fun `minGradleVersion is working`() {
    TestUtils.withSimpleProject { dir ->
      val result = TestUtils.executeGradleWithVersion(dir, MIN_GRADLE_VERSION,"generateApolloSources")

      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)
    }
  }

  @Test
  fun `gradle below minGradleVersion shows an error`() {
    TestUtils.withSimpleProject { dir ->
      var exception: Exception? = null
      try {
        TestUtils.executeGradleWithVersion(dir, "5.4","generateApolloSources")
      } catch (e: UnexpectedBuildFailure) {
        exception = e
        Assert.assertThat(e.message, CoreMatchers.containsString("apollo-android requires Gradle version $MIN_GRADLE_VERSION or greater"))
      }
      Assert.assertNotNull(exception)
    }
  }

  @Test
  fun `gradle 6-0 does not show warnings`() {
    TestUtils.withSimpleProject { dir ->
      val result = TestUtils.executeGradleWithVersion(dir, "6.0","generateApolloSources")

      Assert.assertThat(result.output, not(containsString("Deprecated Gradle features were used in this build")))
    }
  }
}