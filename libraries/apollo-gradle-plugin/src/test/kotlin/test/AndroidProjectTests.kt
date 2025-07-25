package test

import com.apollographql.apollo.gradle.internal.DefaultApolloExtension.Companion.MIN_GRADLE_VERSION
import com.google.common.truth.Truth
import util.TestUtils
import util.TestUtils.executeTaskAndAssertSuccess
import util.TestUtils.withProject
import util.TestUtils.withTestProject
import util.generatedSource
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import util.disableIsolatedProjects
import java.io.File

class AndroidProjectTests {

  @Test
  fun `android library compiles`() {
    withProject(apolloConfiguration = """
      apollo {
        service("service") {
          packageNamesFromFilePaths()
        }
      }
    """.trimIndent(),
        usesKotlinDsl = false,
        plugins = listOf(TestUtils.androidLibraryPlugin, TestUtils.apolloPlugin, TestUtils.kotlinAndroidPlugin)) { dir ->
      val result = TestUtils.executeTask("build", dir)

      assertEquals(TaskOutcome.SUCCESS, result.task(":build")!!.outcome)

      // Java classes generated successfully
      assertTrue(dir.generatedSource("com/example/DroidDetailsQuery.kt").isFile)
      assertTrue(dir.generatedSource("com/example/FilmsQuery.kt").isFile)
      assertTrue(dir.generatedSource("com/example/fragment/SpeciesInformation.kt").isFile)
    }
  }

  @Test
  fun `android application compiles and produces an apk`() {
    withProject(apolloConfiguration = """
      apollo {
        service("service") {
          packageNamesFromFilePaths()
        }
      }
    """.trimIndent(),
        usesKotlinDsl = false,
        plugins = listOf(TestUtils.androidApplicationPlugin, TestUtils.apolloPlugin, TestUtils.kotlinAndroidPlugin)) { dir ->
      val result = TestUtils.executeTask("build", dir)

      assertEquals(TaskOutcome.SUCCESS, result.task(":build")!!.outcome)

      // Java classes generated successfully
      assertTrue(dir.generatedSource("com/example/DroidDetailsQuery.kt").isFile)
      assertTrue(dir.generatedSource("com/example/FilmsQuery.kt").isFile)
      assertTrue(dir.generatedSource("com/example/fragment/SpeciesInformation.kt").isFile)
      assertTrue(File(dir, "build/outputs/apk/debug/testProject-debug.apk").isFile)
    }
  }

  @Test
  fun `android application compiles variants for all flavors, build types and tests`() {
    withTestProject("androidVariants") { dir ->
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
    withTestProject("androidTestVariants") { dir ->
      // compile library variants
      executeTaskAndAssertSuccess(":build", dir)
    }
  }

  /**
   * Using the minimum supported versions of Kotlin for Android should work.
   */
  @Test
  fun `kotlin Android min version succeeds`() {
    withTestProject("kotlin-android-plugin-version") { dir ->
      dir.disableIsolatedProjects()
      /**
       * Use "8.10" because older KGP are not compatible with Gradle 9:
       *
       * ```
       * java.lang.NoClassDefFoundError: org/gradle/api/internal/HasConvention
       * 	at org.jetbrains.kotlin.gradle.plugin.internal.CompatibilityConventionRegistrarG81.addConvention(CompatibilityConventionRegistrarG81.kt:14)
       * ```
       */
      val result = TestUtils.executeGradleWithVersion(dir, "8.10", "build")

      Truth.assertThat(result.task(":build")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
  }

  @Test
  fun `android with java`() {
    withTestProject("android-java") { dir ->
      executeTaskAndAssertSuccess(":assembleDebug", dir)
    }
  }

  /**
   * Using the maximum supported versions of Kotlin for Android should work.
   */
  @Test
  fun `kotlin Android max version succeeds`() {
    withTestProject("kotlin-android-plugin-version") { dir ->
      val gradleScript = dir.resolve("build.gradle.kts")
      gradleScript.writeText(gradleScript.readText().replace("libs.plugins.kotlin.android.min", "libs.plugins.kotlin.android.max"))
      val result = TestUtils.executeTask("build", dir)

      Truth.assertThat(result.task(":build")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
  }
}
