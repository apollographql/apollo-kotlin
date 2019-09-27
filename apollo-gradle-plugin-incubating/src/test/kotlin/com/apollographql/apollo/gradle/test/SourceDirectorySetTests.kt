package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.compiler.child
import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.generatedChild
import org.junit.Assert
import org.junit.Test
import java.io.File

class SourceDirectorySetTests {
  @Test
  fun `android-kotlin builds an apk`() {
    val apolloConfiguration = """
      apollo {
        generateKotlinModels = true
      }
    """.trimIndent()
    TestUtils.withProject(usesKotlinDsl = false,
        apolloConfiguration = apolloConfiguration,
        plugins = listOf(TestUtils.androidApplicationPlugin, TestUtils.kotlinAndroidPlugin, TestUtils.apolloPlugin)) { dir ->

      val source = TestUtils.fixturesDirectory()
      source.child("kotlin").copyRecursively(dir.child("src", "main", "kotlin"))

      TestUtils.executeTask("assembleDebug", dir)

      Assert.assertTrue(File(dir, "build/tmp/kotlin-classes/debug/com/example/DroidDetailsQuery.class").isFile)
      Assert.assertTrue(File(dir, "build/tmp/kotlin-classes/debug/com/example/Main.class").isFile)
      Assert.assertTrue(File(dir, "build/outputs/apk/debug/testProject-debug.apk").isFile)
      Assert.assertTrue(dir.generatedChild("debug/service0/com/example/DroidDetailsQuery.kt").isFile)
    }
  }

  @Test
  fun `non-android-kotlin builds a jar`() {
    val apolloConfiguration = """
      apollo {
        generateKotlinModels = true
      }
    """.trimIndent()
    TestUtils.withProject(usesKotlinDsl = false,
        apolloConfiguration = apolloConfiguration,
        plugins = listOf(TestUtils.javaPlugin, TestUtils.kotlinJvmPlugin, TestUtils.apolloPlugin)) { dir ->

      val source = TestUtils.fixturesDirectory()
      source.child("kotlin").copyRecursively(dir.child("src", "main", "kotlin"))

      TestUtils.executeTask("build", dir)

      Assert.assertTrue(File(dir, "build/classes/kotlin/main/com/example/DroidDetailsQuery.class").isFile)
      Assert.assertTrue(File(dir, "build/classes/kotlin/main/com/example/Main.class").isFile)
      Assert.assertTrue(dir.generatedChild("main/service0/com/example/DroidDetailsQuery.kt").isFile)
      Assert.assertTrue(File(dir, "build/libs/testProject.jar").isFile)
    }
  }

  @Test
  fun `android-java builds an apk`() {
    val apolloConfiguration = """
      apollo {
        generateKotlinModels = false
      }
    """.trimIndent()
    TestUtils.withProject(usesKotlinDsl = false,
        apolloConfiguration = apolloConfiguration,
        plugins = listOf(TestUtils.androidApplicationPlugin, TestUtils.apolloPlugin)) { dir ->

      val source = TestUtils.fixturesDirectory()
      source.child("java").copyRecursively(dir.child("src", "main", "java"))

      TestUtils.executeTask("assembleDebug", dir)

      Assert.assertTrue(File(dir, "build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/com/example/DroidDetailsQuery.class").isFile)
      Assert.assertTrue(File(dir, "build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/com/example/Main.class").isFile)
      Assert.assertTrue(File(dir, "build/outputs/apk/debug/testProject-debug.apk").isFile)
      Assert.assertTrue(dir.generatedChild("debug/service0/com/example/DroidDetailsQuery.java").isFile)
    }
  }

  @Test
  fun `non-android-java builds a jar`() {
    val apolloConfiguration = """
      apollo {
        generateKotlinModels = false
      }
    """.trimIndent()
    TestUtils.withProject(usesKotlinDsl = false,
        apolloConfiguration = apolloConfiguration,
        plugins = listOf(TestUtils.javaPlugin, TestUtils.apolloPlugin)) { dir ->

      val source = TestUtils.fixturesDirectory()
      source.child("java").copyRecursively(dir.child("src", "main", "java"))

      TestUtils.executeTask("build", dir)

      Assert.assertTrue(File(dir, "build/classes/java/main/com/example/DroidDetailsQuery.class").isFile)
      Assert.assertTrue(File(dir, "build/classes/java/main/com/example/Main.class").isFile)
      Assert.assertTrue(dir.generatedChild("main/service0/com/example/DroidDetailsQuery.java").isFile)
      Assert.assertTrue(File(dir, "build/libs/testProject.jar").isFile)
    }
  }
}