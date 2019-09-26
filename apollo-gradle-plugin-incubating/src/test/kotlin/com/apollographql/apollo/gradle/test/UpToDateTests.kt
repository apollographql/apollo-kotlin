package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.TestUtils.withSimpleProject
import com.apollographql.apollo.gradle.util.generatedChild
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UpToDateTests {
  @Test
  fun test() {
    withSimpleProject { dir ->
      `builds successfully and generates expected outputs`(dir)
      `nothing changed, task up to date`(dir)
      `adding a custom type to the build script re-generates the CustomType class`(dir)
    }
  }

  private fun `builds successfully and generates expected outputs`(dir: File) {
    val result = TestUtils.executeTask("generateApolloClasses", dir)

    assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloClasses")!!.outcome)

    // Java classes generated successfully
    assertTrue(dir.generatedChild("main/service0/com/example/DroidDetailsQuery.java").isFile)
    assertTrue(dir.generatedChild("main/service0/com/example/FilmsQuery.java").isFile)
    assertTrue(dir.generatedChild("main/service0/com/example/fragment/SpeciesInformation.java").isFile)

    // verify that the custom type generated was Object.class because no customType mapping was specified
    TestUtils.assertFileContains(dir, "main/service0/com/example/type/CustomType.java", "return Object.class;")

    // Optional is not added to the generated classes
    assert(!TestUtils.fileContains(dir, "main/service0/com/example/DroidDetailsQuery.java", "Optional"))
    TestUtils.assertFileContains(dir, "main/service0/com/example/DroidDetailsQuery.java", "import org.jetbrains.annotations.Nullable;")
  }

  fun `nothing changed, task up to date`(dir: File) {
    val result = TestUtils.executeTask("generateApolloClasses", dir)

    assertEquals(TaskOutcome.UP_TO_DATE, result.task(":generateApolloClasses")!!.outcome)

    // Java classes generated successfully
    assertTrue(dir.generatedChild("main/service0/com/example/DroidDetailsQuery.java").isFile)
    assertTrue(dir.generatedChild("main/service0/com/example/FilmsQuery.java").isFile)
    assertTrue(dir.generatedChild("main/service0/com/example/fragment/SpeciesInformation.java").isFile)
  }

  fun `adding a custom type to the build script re-generates the CustomType class`(dir: File) {
    val apolloBlock = """
      apollo {
        customTypeMapping = ["DateTime": "java.util.Date"]
      }
    """.trimIndent()

    File(dir, "build.gradle").appendText(apolloBlock)

    val result = TestUtils.executeTask("generateApolloClasses", dir)

    // modifying the customTypeMapping should cause the task to be out of date
    // and the task should run again
    assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloClasses")!!.outcome)

    TestUtils.assertFileContains(dir, "main/service0/com/example/type/CustomType.java", "return Date.class;")

    val text = File(dir, "build.gradle").readText()
    File(dir, "build.gradle").writeText(text.replace(apolloBlock, ""))
  }
}