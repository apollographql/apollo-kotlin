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
   * Using older versions of Kotlin will fail, but 1.5 should still work.
   */
  @Test
  fun kotlin15Succeeds() {
    TestUtils.withTestProject("kotlin-plugin-version") { dir ->
      dir.setKotlinPluginVersion("1.5.31")
      val result = TestUtils.executeTask("build", dir)

      Truth.assertThat(result.task(":build")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
  }

}
