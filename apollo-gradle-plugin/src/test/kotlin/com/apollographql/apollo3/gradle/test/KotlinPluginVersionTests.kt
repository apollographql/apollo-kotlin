package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.gradle.util.TestUtils
import com.google.common.truth.Truth
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.io.File

class KotlinPluginVersionTests {
  private fun File.setKotlinPluginVersion(version: String) {
    val gradleScript = resolve("build.gradle.kts")
    gradleScript.writeText(gradleScript.readText().replace("KOTLIN_VERSION", version))
  }

  /**
   * Using older versions of Kotlin will fail, but n-1 should still work.
   */
  @Test
  fun `kotlin n-1 succeeds`() {
    val currentKotlinVersion = System.getenv("COM_APOLLOGRAPHQL_VERSION_KOTLIN_PLUGIN") ?: "1.6"
    val kotlinPluginVersionToTest = if (currentKotlinVersion.startsWith("1.7")) {
      "1.6.0"
    } else {
      "1.5.31"
    }
    TestUtils.withTestProject("kotlin-plugin-version") { dir ->
      dir.setKotlinPluginVersion(kotlinPluginVersionToTest)
      val result = TestUtils.executeTask("build", dir)

      Truth.assertThat(result.task(":build")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
  }

}
