package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.TestUtils.withProject
import com.apollographql.apollo.gradle.util.generatedChild
import com.apollographql.apollo.gradle.util.replaceInText
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AndroidTests {

  @Test
  fun `android library is compiling fine`() {
    withProject(apolloConfiguration = "",
        usesKotlinDsl = false,
        plugins = listOf(TestUtils.androidLibraryPlugin, TestUtils.apolloPlugin)) { dir ->
      val result = TestUtils.executeTask("build", dir)

      assertEquals(TaskOutcome.SUCCESS, result.task(":build")!!.outcome)

      // Java classes generated successfully
      assertTrue(dir.generatedChild("debug/service/com/example/DroidDetailsQuery.java").isFile)
      assertTrue(dir.generatedChild("debug/service/com/example/FilmsQuery.java").isFile)
      assertTrue(dir.generatedChild("debug/service/com/example/fragment/SpeciesInformation.java").isFile)
      assertTrue(dir.generatedChild("release/service/com/example/DroidDetailsQuery.java").isFile)
      assertTrue(dir.generatedChild("release/service/com/example/FilmsQuery.java").isFile)
      assertTrue(dir.generatedChild("release/service/com/example/fragment/SpeciesInformation.java").isFile)
    }
  }

  @Test
  fun `android library debug does not compile release`() {
    withProject(apolloConfiguration = "",
        usesKotlinDsl = false,
        plugins = listOf(TestUtils.androidLibraryPlugin, TestUtils.apolloPlugin)) { dir ->
      val result = TestUtils.executeTask("generateDebugApolloSources", dir)

      assertEquals(TaskOutcome.SUCCESS, result.task(":generateDebugApolloSources")!!.outcome)

      // Java classes generated successfully
      assertTrue(dir.generatedChild("debug/service/com/example/DroidDetailsQuery.java").isFile)
      assertFalse(dir.generatedChild("release/service/com/example/DroidDetailsQuery.java").exists())
    }
  }

  @Test
  fun `android library debug query overrides main`() {
    withProject(apolloConfiguration = "",
        usesKotlinDsl = false,
        plugins = listOf(TestUtils.androidLibraryPlugin, TestUtils.apolloPlugin)) { dir ->

      val debugFile = File(dir, "src/debug/graphql/com/example/DroidDetails.graphql")
      File(dir, "src/main/graphql/com/example/DroidDetails.graphql").copyTo(debugFile)
      debugFile.replaceInText("c3BlY2llczoy", "speciesIdForDebug")

      var exception: Exception? = null
      try {
        TestUtils.executeTask("generateDebugApolloSources", dir)
      } catch (e: UnexpectedBuildFailure) {
        exception = e
        assertThat(e.message, containsString("duplicate(s) graphql file(s) found"))
      }

      assertNotNull(exception)
    }
  }


  @Test
  fun `product flavors compile correctly`() {
    withProject(apolloConfiguration = "",
        usesKotlinDsl = false,
        isFlavored = true,
        plugins = listOf(TestUtils.androidLibraryPlugin, TestUtils.apolloPlugin)) { dir ->

      TestUtils.executeTask("build", dir)

      assertTrue(dir.generatedChild("freeDebug/service/com/example/DroidDetailsQuery.java").isFile)
      assertTrue(dir.generatedChild("freeRelease/service/com/example/DroidDetailsQuery.java").isFile)
      assertTrue(dir.generatedChild("paidDebug/service/com/example/DroidDetailsQuery.java").isFile)
      assertTrue(dir.generatedChild("paidRelease/service/com/example/DroidDetailsQuery.java").isFile)
    }
  }

  @Test
  fun `flavor with invalid schema fails while others compile correctly`() {
    withProject(apolloConfiguration = "",
        usesKotlinDsl = false,
        isFlavored = true,
        plugins = listOf(TestUtils.androidLibraryPlugin, TestUtils.apolloPlugin)) { dir ->

      val freeDebugDir = File(dir, "src/freeDebug/graphql/com/example/")
      freeDebugDir.mkdirs()
      File(freeDebugDir, "schema.json").writeText("This is an invalid schema")

      val paidDebugDir = File(dir, "src/paidDebug/graphql/com/example/")
      paidDebugDir.mkdirs()
      File(dir, "src/main/graphql/com/example/schema.json").copyTo(File(paidDebugDir, "schema.json"))

      File(dir, "src/main/graphql/com/example/schema.json").delete()

      var exception: Exception? = null
      try {
        TestUtils.executeTask("generateFreeDebugApolloSources", dir)
      } catch (e: UnexpectedBuildFailure) {
        exception = e
        assertThat(e.message, containsString("Failed to parse GraphQL schema introspection query"))
      }

      assertNotNull(exception)

      val result = TestUtils.executeTask("generatePaidDebugApolloSources", dir)
      assertEquals(TaskOutcome.SUCCESS, result.task(":generatePaidDebugApolloSources")!!.outcome)
    }
  }

  @Test
  fun `android application is compiling fine`() {
    withProject(apolloConfiguration = "",
        usesKotlinDsl = false,
        plugins = listOf(TestUtils.androidApplicationPlugin, TestUtils.apolloPlugin)) { dir ->
      val result = TestUtils.executeTask("build", dir)

      assertEquals(TaskOutcome.SUCCESS, result.task(":build")!!.outcome)

      // Java classes generated successfully
      assertTrue(dir.generatedChild("debug/service/com/example/DroidDetailsQuery.java").isFile)
      assertTrue(dir.generatedChild("debug/service/com/example/FilmsQuery.java").isFile)
      assertTrue(dir.generatedChild("debug/service/com/example/fragment/SpeciesInformation.java").isFile)
      assertTrue(dir.generatedChild("release/service/com/example/DroidDetailsQuery.java").isFile)
      assertTrue(dir.generatedChild("release/service/com/example/FilmsQuery.java").isFile)
      assertTrue(dir.generatedChild("release/service/com/example/fragment/SpeciesInformation.java").isFile)
    }
  }
}
