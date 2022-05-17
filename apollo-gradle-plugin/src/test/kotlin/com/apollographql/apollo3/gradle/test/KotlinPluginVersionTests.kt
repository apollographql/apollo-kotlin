package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.gradle.util.TestUtils
import com.google.common.truth.Truth
import junit.framework.Assert.fail
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Test
import java.io.File

class KotlinPluginVersionTests {
  private fun File.setKotlinPluginVersion(version: String) {
    val gradleScript = resolve("build.gradle.kts")
    gradleScript.writeText(gradleScript.readText().replace("KOTLIN_VERSION", version))
  }

  @Test
  fun kotlin13Fails() {
    TestUtils.withTestProject("kotlin-plugin-version") { dir ->
      dir.setKotlinPluginVersion("1.3.0")
      // Kotlin 1.3 uses DefaultSourceDirectorySet() which is removed in recent Gradle versions
      try {
        TestUtils.executeGradleWithVersion(dir, "5.6", "generateApolloSources")
        fail("An exception was expected")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("Apollo Kotlin requires Kotlin plugin version 1.4")
      }
    }
  }

  @Test
  fun kotlin14Fails() {
    TestUtils.withTestProject("kotlin-plugin-version") { dir ->
      dir.setKotlinPluginVersion("1.4.32")
      try {
        TestUtils.executeTask("build", dir)
        fail("An exception was expected")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("Incompatible classes were found in dependencies. Remove them from the classpath or use '-Xskip-metadata-version-check' to suppress errors")
      }
    }
  }

  @Test
  fun kotlin15Succeeds() {
    TestUtils.withTestProject("kotlin-plugin-version") { dir ->
      dir.setKotlinPluginVersion("1.5.31")
      val result = TestUtils.executeTask("build", dir)

      Truth.assertThat(result.task(":build")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
  }

}
