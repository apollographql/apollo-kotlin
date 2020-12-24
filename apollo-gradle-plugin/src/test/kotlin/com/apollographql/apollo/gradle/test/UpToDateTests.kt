package com.apollographql.apollo.gradle.test


import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.TestUtils.withSimpleProject
import com.apollographql.apollo.gradle.util.generatedChild
import com.apollographql.apollo.gradle.util.replaceInText
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
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

    // verify that the custom type generated was Any because no customScalarsMapping was specified
    TestUtils.assertFileContains(dir, "service/com/example/type/CustomScalars.kt", "= \"kotlin.Any\"")
  }

  fun `nothing changed, task up to date`(dir: File) {
    val result = TestUtils.executeTask("generateApolloSources", dir)

    assertEquals(TaskOutcome.UP_TO_DATE, result.task(":generateApolloSources")!!.outcome)

    // Java classes generated successfully
    assertTrue(dir.generatedChild("service/com/example/DroidDetailsQuery.kt").isFile)
    assertTrue(dir.generatedChild("service/com/example/FilmsQuery.kt").isFile)
    assertTrue(dir.generatedChild("service/com/example/fragment/SpeciesInformation.kt").isFile)
  }

  fun `adding a custom scalar to the build script re-generates the CustomScalars`(dir: File) {
    val apolloBlock = """
      
      apollo {
        customScalarsMapping = ["DateTime": "java.util.Date"]
      }
    """.trimIndent()

    File(dir, "build.gradle").appendText(apolloBlock)

    val result = TestUtils.executeTask("generateApolloSources", dir)

    // modifying the customScalarsMapping should cause the task to be out of date
    // and the task should run again
    assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)

    TestUtils.assertFileContains(dir, "service/com/example/type/CustomScalars.kt", "\"java.util.Date\"")

    val text = File(dir, "build.gradle").readText()
    File(dir, "build.gradle").writeText(text.replace(apolloBlock, ""))
  }

  @Test
  fun `change graphql file rebuilds the sources`() {
    withSimpleProject { dir ->
      var result = TestUtils.executeTask("generateApolloSources", dir, "-i")

      assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)
      assertThat(dir.generatedChild("service/com/example/DroidDetailsQuery.kt").readText(), containsString("classification"))

      File(dir, "src/main/graphql/com/example/DroidDetails.graphql").replaceInText("classification", "")

      result = TestUtils.executeTask("generateApolloSources", dir, "-i")

      assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)
      assertThat(dir.generatedChild("service/com/example/DroidDetailsQuery.kt").readText(), not(containsString("classification")))
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
