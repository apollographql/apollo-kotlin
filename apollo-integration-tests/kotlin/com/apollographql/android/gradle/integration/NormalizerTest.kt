package com.apollographql.android.gradle.integration

import com.google.common.truth.Truth.assertThat
import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.io.OutputStreamWriter

class NormalizerTest {
  companion object {
    val destDir = createTempTestDirectory("basic")
    @BeforeClass
    @JvmStatic
    fun runBuild() {
      prepareProjectTestDir(destDir, File("../apollo-integration-tests/testFixtures/normalizer"))
    }
  }

  /**
   * For the second run of installApolloCodegen, task outcome should be up-to-date
   */
  @Test
  fun exec() {
    val result = GradleRunner.create()
        .withProjectDir(destDir)
        .withPluginClasspath()
        .withArguments("build")
        .forwardStdError(OutputStreamWriter(System.err))
        .build()
    assertThat(result.task(":build").outcome == TaskOutcome.SUCCESS)
  }
}
