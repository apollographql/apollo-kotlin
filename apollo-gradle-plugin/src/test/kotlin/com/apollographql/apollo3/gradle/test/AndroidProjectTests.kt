package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.gradle.util.TestUtils
import com.apollographql.apollo3.gradle.util.TestUtils.executeTaskAndAssertSuccess
import com.apollographql.apollo3.gradle.util.TestUtils.withProject
import com.apollographql.apollo3.gradle.util.TestUtils.withTestProject
import com.apollographql.apollo3.gradle.util.generatedChild
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AndroidProjectTests {

  @Test
  fun `android library compiles`() {
    withProject(apolloConfiguration = "",
        usesKotlinDsl = false,
        plugins = listOf(TestUtils.androidLibraryPlugin, TestUtils.apolloPlugin, TestUtils.kotlinAndroidPlugin)) { dir ->
      val result = TestUtils.executeTask("build", dir)

      assertEquals(TaskOutcome.SUCCESS, result.task(":build")!!.outcome)

      // Java classes generated successfully
      assertTrue(dir.generatedChild("service/DroidDetailsQuery.kt").isFile)
      assertTrue(dir.generatedChild("service/FilmsQuery.kt").isFile)
      assertTrue(dir.generatedChild("service/fragment/SpeciesInformation.kt").isFile)
    }
  }


  @Test
  fun `android application compiles and produces an apk`() {
    withProject(apolloConfiguration = "",
        usesKotlinDsl = false,
        plugins = listOf(TestUtils.androidApplicationPlugin, TestUtils.apolloPlugin, TestUtils.kotlinAndroidPlugin)) { dir ->
      val result = TestUtils.executeTask("build", dir)

      assertEquals(TaskOutcome.SUCCESS, result.task(":build")!!.outcome)

      // Java classes generated successfully
      assertTrue(dir.generatedChild("service/DroidDetailsQuery.kt").isFile)
      assertTrue(dir.generatedChild("service/FilmsQuery.kt").isFile)
      assertTrue(dir.generatedChild("service/fragment/SpeciesInformation.kt").isFile)
      assertTrue(File(dir, "build/outputs/apk/debug/testProject-debug.apk").isFile)
    }
  }

  @Test
  fun `android application compiles variants for all flavors, build types and tests`() {
    withTestProject("androidVariants") {dir ->
      // compile library variants
      executeTaskAndAssertSuccess(":compileDemoDebugKotlin", dir)
      executeTaskAndAssertSuccess(":compileDemoReleaseKotlin", dir)
      executeTaskAndAssertSuccess(":compileFullDebugKotlin", dir)
      executeTaskAndAssertSuccess(":compileFullReleaseKotlin", dir)

      // compile test variants.
      // Unit test are compiled for debug + release, UI tests are only debug
      executeTaskAndAssertSuccess(":compileDemoDebugUnitTestKotlin", dir)
      executeTaskAndAssertSuccess(":compileDemoReleaseUnitTestKotlin", dir)
      executeTaskAndAssertSuccess(":compileFullDebugUnitTestKotlin", dir)
      executeTaskAndAssertSuccess(":compileFullReleaseUnitTestKotlin", dir)
      executeTaskAndAssertSuccess(":compileDemoDebugAndroidTestKotlin", dir)
      executeTaskAndAssertSuccess(":compileFullDebugAndroidTestKotlin", dir)
    }
  }

  @Test
  fun `can connect outputDir to tests`() {
    withTestProject("androidTestVariants") {dir ->
      // compile library variants
      executeTaskAndAssertSuccess(":build", dir)
    }
  }
}
