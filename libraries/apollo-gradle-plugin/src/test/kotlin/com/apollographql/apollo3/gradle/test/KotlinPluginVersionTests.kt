package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.gradle.util.TestUtils
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
    TestUtils.withTestProject("kotlin-plugin-version") { dir ->
      val result = TestUtils.executeTask("build", dir)

      Truth.assertThat(result.task(":build")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
  }

  /**
   * Using the minimum supported versions of Kotlin for Android should work.
   */
  @Test
  fun `kotlin Android min version succeeds`() {
    TestUtils.withTestProject("kotlin-android-plugin-version") { dir ->
      val result = TestUtils.executeTask("build", dir)

      Truth.assertThat(result.task(":build")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
  }

  /**
   * Using the maximum supported versions of Kotlin for Android should work.
   */
  @Test
  fun `kotlin Android max version succeeds`() {
    TestUtils.withTestProject("kotlin-android-plugin-version") { dir ->
      val gradleScript = dir.resolve("build.gradle.kts")
      gradleScript.writeText(gradleScript.readText().replace("libs.plugins.kotlin.android.min", "libs.plugins.kotlin.android.max"))
      val result = TestUtils.executeTask("build", dir)

      Truth.assertThat(result.task(":build")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
  }

}
