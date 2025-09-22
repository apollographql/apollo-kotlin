package test

import util.TestUtils
import util.TestUtils.withSimpleProject
import util.generatedSource
import util.replaceInText
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Test
import util.TestUtils.setVersionsUnderTest
import util.TestUtils.withTestProject
import util.VersionsUnderTest
import util.agp8_13_kgp_2_2_20
import util.agp8_kgp1_9
import java.io.File
import kotlin.test.assertEquals

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

  fun jvmLibrary(versionsUnderTest: VersionsUnderTest?) {
    withTestProject("jvm-library") { dir ->
      setVersionsUnderTest(dir, versionsUnderTest)

      val result = TestUtils.executeGradleWithVersion(dir, versionsUnderTest?.gradle, ":build")
      assertEquals(TaskOutcome.SUCCESS, result.task(":build")?.outcome)
      Assert.assertTrue(dir.generatedSource("com/example/GetFooQuery.kt").isFile)
    }
  }

  @Test
  fun jvmLibrary() = jvmLibrary(null)

  @Test
  fun jvmLibrary_1_9_0() = jvmLibrary(agp8_kgp1_9)

  @Test
  fun jvmLibrary_2_2_20() = jvmLibrary(agp8_13_kgp_2_2_20)

  @Test
  fun `generated models can be added to all source sets`() {
    withTestProject("kotlinJvmSourceSets") { dir ->
      // Order is important as compileTestKotlin depends on compileKotlin
      TestUtils.executeTaskAndAssertSuccess(":compileKotlin", dir)
      TestUtils.executeTaskAndAssertSuccess(":compileTestKotlin", dir)
    }
  }
}