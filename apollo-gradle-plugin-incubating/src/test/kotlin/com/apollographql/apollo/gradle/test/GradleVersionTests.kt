package com.apollographql.apollo.gradle.test

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
  fun `gradle 5-6 is working`() {
    TestUtils.withSimpleProject { dir ->
      val result = TestUtils.executeGradleWithVersion(dir, "5.6","generateApolloSources")

      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)
    }
  }

  @Test
  fun `gradle below 5-6 shows an error`() {
    TestUtils.withSimpleProject { dir ->
      var exception: Exception? = null
      try {
        TestUtils.executeGradleWithVersion(dir, "5.5","generateApolloSources")
      } catch (e: UnexpectedBuildFailure) {
        exception = e
        Assert.assertThat(e.message, CoreMatchers.containsString("apollo-android requires Gradle version 5.6 or greater"))
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