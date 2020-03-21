package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.internal.child
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
      Assert.assertTrue(dir.generatedChild("debug/service/com/example/DroidDetailsQuery.kt").isFile)
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
      Assert.assertTrue(dir.generatedChild("main/service/com/example/DroidDetailsQuery.kt").isFile)
      Assert.assertTrue(File(dir, "build/libs/testProject.jar").isFile)
    }
  }

  @Test
  fun `pure-jvm java models are reachable from kotlin code`() {
    val apolloConfiguration = """
      apollo {
        generateKotlinModels = false
      }
    """.trimIndent()
    TestUtils.withProject(usesKotlinDsl = false,
        apolloConfiguration = apolloConfiguration,
        plugins = listOf(TestUtils.javaPlugin, TestUtils.kotlinJvmPlugin, TestUtils.apolloPlugin)) { dir ->

      val source = TestUtils.fixturesDirectory()
      source.child("kotlin").copyRecursively(dir.child("src", "main", "kotlin"))

      TestUtils.executeTask("build", dir)

      Assert.assertTrue(File(dir, "build/classes/java/main/com/example/DroidDetailsQuery.class").isFile)
      Assert.assertTrue(File(dir, "build/classes/kotlin/main/com/example/Main.class").isFile)
      Assert.assertTrue(dir.generatedChild("main/service/com/example/DroidDetailsQuery.java").isFile)
      Assert.assertTrue(File(dir, "build/libs/testProject.jar").isFile)
    }
  }

  @Test
  fun `android java models are reachable from kotlin code`() {
    val apolloConfiguration = """
      apollo {
        generateKotlinModels = false
      }
    """.trimIndent()
    TestUtils.withProject(usesKotlinDsl = false,
        apolloConfiguration = apolloConfiguration,
        plugins = listOf(TestUtils.androidApplicationPlugin, TestUtils.kotlinAndroidPlugin, TestUtils.apolloPlugin)) { dir ->

      val source = TestUtils.fixturesDirectory()
      source.child("kotlin").copyRecursively(dir.child("src", "main", "kotlin"))

      TestUtils.executeTask("assembleDebug", dir)

      Assert.assertTrue(File(dir, "build/intermediates/javac/debug/classes/com/example/DroidDetailsQuery.class").isFile)
      Assert.assertTrue(File(dir, "build/tmp/kotlin-classes/debug/com/example/Main.class").isFile)
      Assert.assertTrue(File(dir, "build/outputs/apk/debug/testProject-debug.apk").isFile)
      Assert.assertTrue(dir.generatedChild("debug/service/com/example/DroidDetailsQuery.java").isFile)
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

      Assert.assertTrue(File(dir, "build/intermediates/javac/debug/classes/com/example/DroidDetailsQuery.class").isFile)
      Assert.assertTrue(File(dir, "build/intermediates/javac/debug/classes/com/example/Main.class").isFile)
      Assert.assertTrue(File(dir, "build/outputs/apk/debug/testProject-debug.apk").isFile)
      Assert.assertTrue(dir.generatedChild("debug/service/com/example/DroidDetailsQuery.java").isFile)
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
      Assert.assertTrue(dir.generatedChild("main/service/com/example/DroidDetailsQuery.java").isFile)
      Assert.assertTrue(File(dir, "build/libs/testProject.jar").isFile)
    }
  }

  @Test
  fun `non-android jvm can add queries to the test variant`() {
    val apolloConfiguration = """
      apollo {
        generateKotlinModels = false
      }
    """.trimIndent()
    TestUtils.withProject(usesKotlinDsl = false,
        apolloConfiguration = apolloConfiguration,
        plugins = listOf(TestUtils.javaPlugin, TestUtils.apolloPlugin)) { dir ->

      val source = TestUtils.fixturesDirectory()
      source.child("java").copyRecursively(dir.child("src", "test", "java"))

      dir.child("src/main/graphql").copyRecursively(dir.child("src/test/graphql"))
      dir.child("src/main/graphql").deleteRecursively()

      TestUtils.executeTask("test", dir)

      Assert.assertTrue(File(dir, "build/classes/java/test/com/example/DroidDetailsQuery.class").isFile)
      Assert.assertTrue(File(dir, "build/classes/java/test/com/example/Main.class").isFile)
      Assert.assertTrue(dir.generatedChild("test/service/com/example/DroidDetailsQuery.java").isFile)
    }
  }

  @Test
  fun `non-android jvm can add additional sourceSets`() {
    val apolloConfiguration = """
      sourceSets {
        foo {
        }
      }
      dependencies {
        fooImplementation dep.jetbrainsAnnotations
        fooImplementation dep.apollo.api
      }
      apollo {
        generateKotlinModels = false
      }
    """.trimIndent()
    TestUtils.withProject(usesKotlinDsl = false,
        apolloConfiguration = apolloConfiguration,
        plugins = listOf(TestUtils.javaPlugin, TestUtils.apolloPlugin)) { dir ->

      val source = TestUtils.fixturesDirectory()
      source.child("java").copyRecursively(dir.child("src", "foo", "java"))

      dir.child("src/main/graphql").copyRecursively(dir.child("src/foo/graphql"))
      dir.child("src/main/graphql").deleteRecursively()

      val result = TestUtils.executeTask("compileFooJava", dir)
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":compileFooJava")!!.outcome)

      Assert.assertTrue(File(dir, "build/classes/java/foo/com/example/DroidDetailsQuery.class").isFile)
      Assert.assertTrue(File(dir, "build/classes/java/foo/com/example/Main.class").isFile)
      Assert.assertTrue(dir.generatedChild("foo/service/com/example/DroidDetailsQuery.java").isFile)

      // Also verify that the compile task is compileJava and not compileMainJava
      val resultMain = TestUtils.executeTask("compileJava", dir)
      Assert.assertEquals(TaskOutcome.NO_SOURCE, resultMain.task(":compileJava")!!.outcome)
    }
  }

  @Test
  fun `custom sourceSets with dash does not make configuration fail`() {
    val apolloConfiguration = """
      sourceSets {
        'native-test' {
        }
      }
    """.trimIndent()
    TestUtils.withProject(apolloConfiguration = apolloConfiguration,
        plugins = listOf(TestUtils.javaPlugin, TestUtils.apolloPlugin),
        usesKotlinDsl = false) { dir ->

      val result = TestUtils.executeTask("compileNativeTestJava", dir)
      Assert.assertEquals(TaskOutcome.NO_SOURCE, result.task(":compileNativeTestJava")!!.outcome)
    }
  }

  @Test
  fun `android can add queries to the test variant`() {
    val apolloConfiguration = """
      apollo {
        generateKotlinModels = false
      }
    """.trimIndent()
    TestUtils.withProject(usesKotlinDsl = false,
        apolloConfiguration = apolloConfiguration,
        plugins = listOf(TestUtils.javaPlugin, TestUtils.apolloPlugin)) { dir ->

      val source = TestUtils.fixturesDirectory()
      source.child("java").copyRecursively(dir.child("src", "test", "java"))

      dir.child("src/main/graphql").copyRecursively(dir.child("src/test/graphql"))
      dir.child("src/main/graphql").deleteRecursively()

      TestUtils.executeTask("test", dir)

      Assert.assertTrue(File(dir, "build/classes/java/test/com/example/DroidDetailsQuery.class").isFile)
      Assert.assertTrue(File(dir, "build/classes/java/test/com/example/Main.class").isFile)
      Assert.assertTrue(dir.generatedChild("test/service/com/example/DroidDetailsQuery.java").isFile)
    }
  }

  @Test
  fun `android can add queries to the androidTest variant`() {
    val apolloConfiguration = """
      apollo {
        generateKotlinModels = false
      }
    """.trimIndent()
    TestUtils.withProject(usesKotlinDsl = false,
        apolloConfiguration = apolloConfiguration,
        plugins = listOf(TestUtils.androidApplicationPlugin, TestUtils.apolloPlugin)) { dir ->

      val source = TestUtils.fixturesDirectory()
      source.child("java").copyRecursively(dir.child("src", "androidTest", "java"))

      dir.child("src/main/graphql").copyRecursively(dir.child("src/androidTest/graphql"))
      dir.child("src/main/graphql").deleteRecursively()

      TestUtils.executeTask("assembleAndroidTest", dir)

      Assert.assertTrue(dir.generatedChild("debugAndroidTest/service/com/example/DroidDetailsQuery.java").isFile)
      Assert.assertTrue(dir.child("build/outputs/apk/androidTest/debug/testProject-debug-androidTest.apk").isFile)

    }
  }
}
