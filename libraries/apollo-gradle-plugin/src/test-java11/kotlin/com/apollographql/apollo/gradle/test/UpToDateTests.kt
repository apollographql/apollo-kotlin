package com.apollographql.apollo.gradle.test


import util.TestUtils
import util.TestUtils.withSimpleProject
import util.generatedChild
import util.replaceInText
import com.google.common.truth.Truth
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UpToDateTests {
  @Test
  fun `complete test`() {
    withSimpleProject { dir ->
      `builds successfully and generates expected outputs`(dir)
      `nothing changed, task up to date`(dir)
      `adding a custom scalar to the build script re-generates the CustomScalars`(dir)
    }
  }

  private fun `builds successfully and generates expected outputs`(dir: File) {
    val result = TestUtils.executeTask("generateApolloSources", dir)

    assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)

    // Java classes generated successfully
    assertTrue(dir.generatedChild("service/com/example/DroidDetailsQuery.kt").isFile)
    assertTrue(dir.generatedChild("service/com/example/FilmsQuery.kt").isFile)
    assertTrue(dir.generatedChild("service/com/example/fragment/SpeciesInformation.kt").isFile)
  }

  private fun `nothing changed, task up to date`(dir: File) {
    val result = TestUtils.executeTask("generateApolloSources", dir)

    assertEquals(TaskOutcome.UP_TO_DATE, result.task(":generateApolloSources")!!.outcome)

    // Java classes generated successfully
    assertTrue(dir.generatedChild("service/com/example/DroidDetailsQuery.kt").isFile)
    assertTrue(dir.generatedChild("service/com/example/FilmsQuery.kt").isFile)
    assertTrue(dir.generatedChild("service/com/example/fragment/SpeciesInformation.kt").isFile)
  }

  private fun `adding a custom scalar to the build script re-generates the CustomScalars`(dir: File) {
    File(dir, "build.gradle").replaceInText("packageNamesFromFilePaths()", """
      packageNamesFromFilePaths()
      mapScalar("DateTime", "java.util.Date")
    """.trimIndent())

    val result = TestUtils.executeTask("generateApolloSources", dir)

    // modifying the scalars mapping should cause the task to be out of date
    // and the task should run again
    assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)

    TestUtils.assertFileContains(dir, "service/com/example/type/DateTime.kt", "\"java.util.Date\"")

    File(dir, "build.gradle").replaceInText("mapScalar(\"DateTime\", \"java.util.Date\")", "")
  }

  @Test
  fun `change graphql file rebuilds the sources`() {
    withSimpleProject { dir ->
      var result = TestUtils.executeTask("generateApolloSources", dir, "-i")

      assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)
      Truth.assertThat(dir.generatedChild("service/com/example/DroidDetailsQuery.kt").readText()).contains("classification")

      File(dir, "src/main/graphql/com/example/DroidDetails.graphql").replaceInText("classification", "")

      result = TestUtils.executeTask("generateApolloSources", dir, "-i")

      assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)
      Truth.assertThat(dir.generatedChild("service/com/example/DroidDetailsQuery.kt").readText()).doesNotContain("classification")
    }
  }

  @Test
  fun `change schema file rebuilds the sources`() {
    withSimpleProject { dir ->
      var result = TestUtils.executeTask("generateApolloSources", dir, "-i")

      assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)

      val schemaFile = File(dir, "src/main/graphql/com/example/schema.json")
      schemaFile.replaceInText("The ID of an object", "The ID of an object (modified)")

      result = TestUtils.executeTask("generateApolloSources", dir, "-i")

      assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)
    }
  }
}
