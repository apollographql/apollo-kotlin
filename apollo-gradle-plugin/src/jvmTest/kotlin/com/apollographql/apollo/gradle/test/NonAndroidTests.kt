package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.TestUtils.withSimpleProject
import com.apollographql.apollo.gradle.util.replaceInText
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Test
import java.io.File

class NonAndroidTests {
  @Test
  fun `applying the apollo plugin does not pull the android plugin in the classpath`() {
    withSimpleProject { dir ->
      // Remove the google() repo where the android plugin resides
      File(dir, "build.gradle").replaceInText("google()", "")

      val result = TestUtils.executeTask("generateApolloSources", dir)

      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)
    }
  }
}