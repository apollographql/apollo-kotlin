package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.compiler.child
import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.TestUtils.withProject
import com.apollographql.apollo.gradle.util.generatedChild
import org.junit.Assert
import org.junit.Test
import java.io.File

class KotlinCodegenTests {
  @Test
  fun `generates and compiles kotlin`() {
    val apolloConfiguration = """
      apollo {
        generateKotlinModels = true
      }
    """.trimIndent()
    withProject(usesKotlinDsl = false,
        apolloConfiguration = apolloConfiguration,
        plugins = listOf(TestUtils.javaPlugin, TestUtils.kotlinJvmPlugin, TestUtils.apolloPlugin)) { dir ->

      val source = TestUtils.fixturesDirectory()
      source.child("kotlin").copyRecursively(dir.child("src", "main", "kotlin"))

      TestUtils.executeTask("build", dir)
      Assert.assertTrue(File(dir, "build/classes/kotlin/main/com/example/DroidDetailsQuery.class").isFile)
      Assert.assertTrue(dir.generatedChild("main/service0/com/example/DroidDetailsQuery.kt").isFile)
      Assert.assertTrue(dir.generatedChild("main/service0/com/example/type/CustomType.kt").isFile)
      Assert.assertTrue(dir.generatedChild("main/service0/com/example/fragment/SpeciesInformation.kt").isFile)
    }
  }
}