package com.apollographql.android.gradle.integration

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.io.OutputStreamWriter
import java.nio.file.*
import java.nio.file.FileVisitResult.CONTINUE
import java.nio.file.attribute.BasicFileAttributes

@RunWith(Parameterized::class)
class FixturesIntegrationTest(val fixtureDir: File, val fixtureName: String) {
  private val expectedFileMatcher = FileSystems.getDefault().getPathMatcher("glob:**.java")

  @Test
  fun testExpectedOutput() {
    val destDir = createTempTestDirectory(fixtureName)
    prepareProjectTestDir(destDir, fixtureDir)

    val result = GradleRunner.create()
        .withProjectDir(destDir)
        .withPluginClasspath()
        .withArguments("clean", "build", "-Dapollographql.skipApi=true")
        .forwardStdError(OutputStreamWriter(System.err))
        .build()

    assertThat(result.task(":build").outcome == TaskOutcome.SUCCESS)

    val expectedDir = File(destDir, "expected/").toPath()
    val outputDir = File(destDir, "build/generated/source/apollo/").toPath()

    Files.walkFileTree(expectedDir, object : SimpleFileVisitor<Path>() {
      override fun visitFile(expectedFile: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (expectedFileMatcher.matches(expectedFile)) {
          val actual = outputDir.resolve(expectedDir.relativize(expectedFile).toString())
          if (!Files.exists(actual)) {
            throw AssertionError("Couldn't find generated for: $actual")
          }
          assertThat(actual.toFile().readText()).isEqualTo(expectedFile.toFile().readText())
        }
        return CONTINUE
      }
    })
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{1}")
    fun data(): Collection<Array<Any>> {
      return File("../apollo-integration-tests/testFixtures").listFiles()
          .filter { it.isDirectory }
          .map { arrayOf(it, it.name) }
    }
  }
}
