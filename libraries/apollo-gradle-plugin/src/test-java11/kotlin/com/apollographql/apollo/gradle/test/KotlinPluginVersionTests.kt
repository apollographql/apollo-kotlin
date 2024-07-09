package com.apollographql.apollo.gradle.test

import util.TestUtils
import com.google.common.truth.Truth
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.io.File

class KotlinPluginVersionTests {

  /**
   * Using the minimum supported versions of Kotlin for JVM should work.
   */
  @Test
  fun `kotlin JVM min version succeeds`() {
    TestUtils.withTestProject("kotlin-plugin-version-min") { dir ->
      val result = TestUtils.executeTask("build", dir)

      Truth.assertThat(result.task(":build")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
  }
}
