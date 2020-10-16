package com.apollographql.apollo.gradle.test


import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.generatedChild
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Test
import java.io.File

class SourceDirectorySetTests {
  @Test
  fun `android-kotlin builds an apk`() {
    val apolloConfiguration = """
      apollo {
      }
    """.trimIndent()
    TestUtils.withProject(usesKotlinDsl = false,
        apolloConfiguration = apolloConfiguration,
        plugins = listOf(TestUtils.androidApplicationPlugin, TestUtils.kotlinAndroidPlugin, TestUtils.apolloPlugin)) { dir ->

      val source = TestUtils.fixturesDirectory()
      File(source, "kotlin").copyRecursively(File(dir, "src/main/kotlin"))

      TestUtils.executeTask("assembleDebug", dir)

      Assert.assertTrue(File(dir, "build/outputs/apk/debug/testProject-debug.apk").isFile)
      Assert.assertTrue(dir.generatedChild("service/com/example/DroidDetailsQuery.kt").isFile)
    }
  }

  @Test
  fun `non-android-kotlin builds a jar`() {
    val apolloConfiguration = """
      apollo {
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
  fun `withOutputDir can rewire to the test source set`() {
    TestUtils.withTestProject("testSourceSet") {dir ->
      TestUtils.executeTask("build", dir)

      Assert.assertTrue(dir.generatedChild("service/com/example/GreetingQuery.kt").isFile)
      Assert.assertTrue(File(dir, "build/libs/testProject.jar").isFile)
    }
  }
}
