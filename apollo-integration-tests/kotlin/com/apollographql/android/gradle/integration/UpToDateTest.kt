package com.apollographql.android.gradle.integration

import com.google.common.truth.Truth.assertThat
import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.io.OutputStreamWriter

/**
 * Verifies the up-to-date checks for the gradle plugin tasks
 */
class UpToDateTest {
  companion object {
    val destDir = createTempTestDirectory("basic")
    @BeforeClass
    @JvmStatic
    fun runBuild() {
      prepareProjectTestDir(destDir, File("../apollo-integration-tests/testFixtures/basic"))
      GradleRunner.create()
          .withProjectDir(destDir)
          .withPluginClasspath()
          .withArguments("clean", "build", "-Dapollographql.skipApi=true", "--stacktrace", "--debug")
          .forwardStdError(OutputStreamWriter(System.err))
          .build()
    }
  }

  /**
   * For the second run of installApolloCodegen, task outcome should be up-to-date
   */
  @Test
  fun testApolloCodegen() {
    val result = GradleRunner.create()
        .withProjectDir(destDir)
        .withPluginClasspath()
        .withArguments("installApolloCodegen")
        .forwardStdError(OutputStreamWriter(System.err))
        .build()
    assertThat(result.task(":installApolloCodegen").outcome == TaskOutcome.UP_TO_DATE)
  }

  /**
   * On deleting node_modules directory, installApolloCodegen should run again and outcome should be success
   */
  @Test
  fun testApolloCodegenWhenNodeModulesDeleted() {
    FileUtils.deleteDirectory(File(destDir, "node_modules"))

    val result = GradleRunner.create()
        .withProjectDir(destDir)
        .withPluginClasspath()
        .withArguments("installApolloCodegen")
        .forwardStdError(OutputStreamWriter(System.err))
        .build()
    assertThat(result.task(":installApolloCodegen").outcome == TaskOutcome.SUCCESS)
  }

  /**
   * generateDebugApolloClasses should be up-to-date after running build
   */
  @Test
  fun testGenerateDebugApolloClasses() {
    FileUtils.deleteDirectory(File(destDir, "node_modules"))

    val result = GradleRunner.create()
        .withProjectDir(destDir)
        .withPluginClasspath()
        .withArguments("generateDebugApolloClasses")
        .forwardStdError(OutputStreamWriter(System.err))
        .build()
    assertThat(result.task(":generateDebugApolloClasses").outcome == TaskOutcome.UP_TO_DATE)
  }
}
