package com.apollographql.apollo.gradle.util

import org.gradle.testkit.runner.GradleRunner
import java.io.File

object TestUtils {
  fun withGradleRunner(source: String, block: (File, GradleRunner) -> Unit) = withTestDirectory(source) { dir ->

    val runner = GradleRunner.create()
        .withPluginClasspath()
        .forwardStdOutput(System.out.writer())
        .forwardStdError(System.err.writer())
        .withProjectDir(dir)

    block(dir, runner)
  }

  fun withTestDirectory(source: String, block: (File) -> Unit) {
    val s = File(System.getProperty("user.dir"), "src/test/testProjects/$source")
    val dir = File(System.getProperty("user.dir"), "build/testProjects/$source")

    s.copyRecursively(target = dir, overwrite = true)

    // This is not strictly needed for non-android project but doesn't harm either
    File(dir, "local.properties").writeText("sdk.dir=${androidHome()}\n")

    block(dir)

    dir.deleteRecursively()
  }
}