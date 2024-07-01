package com.apollographql.apollo.gradle.test

import util.TestUtils
import util.TestUtils.withSimpleProject
import util.generatedChild
import util.replaceInText
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Test
import java.io.File

class KotlinJVMProjectTests {
  @Test
  fun `applying the apollo plugin does not pull the android plugin in the classpath`() {
    withSimpleProject { dir ->
      // Remove the google() repo where the android plugin resides
      File(dir, "build.gradle").replaceInText("google()", "")

      val result = TestUtils.executeTask("generateApolloSources", dir)

      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)
    }
  }

  @Test
  fun `non-android-kotlin builds a jar`() {
    val apolloConfiguration = """
      apollo {
        service("service") {
          packageNamesFromFilePaths()
        }
      }
    """.trimIndent()
    TestUtils.withProject(usesKotlinDsl = false,
        apolloConfiguration = apolloConfiguration,
        plugins = listOf(TestUtils.kotlinJvmPlugin, TestUtils.apolloPlugin)) { dir ->

      val source = TestUtils.fixturesDirectory()
      File(source, "kotlin").copyRecursively(File(dir, "src/main/kotlin"))

      TestUtils.executeTask("build", dir)

      Assert.assertTrue(File(dir, "build/classes/kotlin/main/com/example/DroidDetailsQuery.class").isFile)
      Assert.assertTrue(File(dir, "build/classes/kotlin/main/com/example/Main.class").isFile)
      Assert.assertTrue(dir.generatedChild("service/com/example/DroidDetailsQuery.kt").isFile)
      Assert.assertTrue(File(dir, "build/libs/testProject.jar").isFile)
    }
  }

  @Test
  fun `generated models can be added to all source sets`() {
    TestUtils.withTestProject("kotlinJvmSourceSets") { dir ->
      // Order is important as compileTestKotlin depends on compileKotlin
      TestUtils.executeTaskAndAssertSuccess(":compileKotlin", dir)
      TestUtils.executeTaskAndAssertSuccess(":compileTestKotlin", dir)
    }
  }
}