package test

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import util.TestUtils
import util.TestUtils.executeGradleWithVersion
import util.TestUtils.executeTask
import util.TestUtils.executeTaskAndAssertSuccess
import util.TestUtils.setVersionsUnderTest
import util.TestUtils.withTestProject
import util.VersionsUnderTest
import util.agp8_kgp2_1_0
import util.agp8_13_0_versions
import util.agp9_versions
import util.generatedSource

class AndroidProjectTests {
  private fun androidLibrary(versionsUnderTest: VersionsUnderTest?) {
    withTestProject("android-library") { dir ->
      setVersionsUnderTest(dir, versionsUnderTest)
      val result = executeGradleWithVersion(dir, versionsUnderTest?.gradle, "compileDebugKotlin")
      assertEquals(TaskOutcome.SUCCESS, result.task(":compileDebugKotlin")!!.outcome)
      assertTrue(dir.generatedSource("com/example/GetFooQuery.kt").isFile)
    }
  }

  @Test
  fun androidLibrary() = androidLibrary(null)

  @Test
  fun androidLibrary_8_0_0() = androidLibrary(agp8_kgp2_1_0)

  @Test
  fun androidLibrary_8_13_0() = androidLibrary(agp8_13_0_versions)

  @Test
  fun androidLibrary_9_0_0() = androidLibrary(agp9_versions)

  private fun androidVariants(versionsUnderTest: VersionsUnderTest?) {
    withTestProject("android-variants") { dir ->
      setVersionsUnderTest(dir, versionsUnderTest)
      val tasks = buildList {
        add(":compileDemoDebugKotlin")
        add(":compileDemoReleaseKotlin")
        add(":compileFullDebugKotlin")
        add(":compileFullReleaseKotlin")
        add(":compileDemoDebugUnitTestKotlin")
        if (versionsUnderTest?.agp?.startsWith("9") != true) {
          // AGP 9 doesn't create release unit tests?
          add(":compileDemoReleaseUnitTestKotlin")
          add(":compileFullReleaseUnitTestKotlin")
        }
        add(":compileFullDebugUnitTestKotlin")
        add(":compileDemoDebugAndroidTestKotlin")
        add(":compileFullDebugAndroidTestKotlin")
      }
      val result = executeGradleWithVersion(dir, versionsUnderTest?.gradle, *tasks.toTypedArray())

      tasks.forEach {
        assertEquals(TaskOutcome.SUCCESS, result.task(it)?.outcome)
      }
    }
  }

  @Test
  fun androidVariants() = androidVariants(null)

  @Test
  fun androidVariants_8_0_0() = androidVariants(agp8_kgp2_1_0)

  @Test
  fun androidVariants_8_13_0() = androidVariants(agp8_13_0_versions)

  @Test
  fun androidVariants_9_0_0() = androidVariants(agp9_versions)

  fun androidJava(versionsUnderTest: VersionsUnderTest?) {
    withTestProject("android-java") { dir ->
      TestUtils.setVersionsUnderTest(dir, versionsUnderTest)
      val task = ":compileDebugJavaWithJavac"
      val result = executeGradleWithVersion(dir, versionsUnderTest?.gradle, task)
      assertEquals(TaskOutcome.SUCCESS, result.task(task)?.outcome)
    }
  }

  @Test
  fun androidJava() = androidJava(null)

  @Test
  fun androidJava_8_0_0() = androidJava(agp8_kgp2_1_0)

  @Test
  fun androidJava_8_13_0() = androidJava(agp8_13_0_versions)

  @Test
  fun androidJava_9_0_0() = androidJava(agp9_versions)

  fun android9Kmp(versionsUnderTest: VersionsUnderTest?) {
    withTestProject("android-9-kmp") { dir ->
      setVersionsUnderTest(dir, versionsUnderTest)
      val result = executeGradleWithVersion(dir, versionsUnderTest?.gradle, ":build")
      assertEquals(TaskOutcome.SUCCESS, result.task(":build")!!.outcome)
    }
  }

  @Test
  fun android9Kmp() = android9Kmp(agp9_versions)

  @Test
  fun androidApplication() {
    withTestProject("android-application") { dir ->
      val result = executeTask("compileDebugKotlin", dir)
      assertEquals(TaskOutcome.SUCCESS, result.task(":compileDebugKotlin")!!.outcome)
      assertTrue(dir.generatedSource("com/example/GetFooQuery.kt").isFile)
    }
  }

  @Test
  fun androidTestVariants() {
    withTestProject("android-test-variants") { dir ->
      // compile library variants
      executeTaskAndAssertSuccess(":build", dir)
    }
  }
}
