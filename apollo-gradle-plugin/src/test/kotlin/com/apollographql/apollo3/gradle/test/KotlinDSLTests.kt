package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.gradle.util.TestUtils
import com.apollographql.apollo3.gradle.util.generatedChild
import com.google.common.truth.Truth
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class KotlinDSLTests {
  @Test
  fun `generated accessors work as expected`() {
    val apolloConfiguration = """
      apollo {
      }
    """.trimIndent()

    TestUtils.withGeneratedAccessorsProject(apolloConfiguration) {dir ->
      TestUtils.executeGradle(dir)
    }
  }

  @Test
  fun `generated accessors do not expose DefaultApolloExtension`() {
    val apolloConfiguration = """
      apollo {
        println("apollo has ${'$'}{services.size} services")
      }
    """.trimIndent()

    TestUtils.withGeneratedAccessorsProject(apolloConfiguration) {dir ->
      var exception: Exception? = null
      try {
        TestUtils.executeGradle(dir)
      } catch (e: UnexpectedBuildFailure) {
        exception = e
        Truth.assertThat(e.message).contains("Unresolved reference: services")
      }
      Assert.assertNotNull(exception)
    }
  }

  @Test
  fun `parameters do not throw`() {
    val apolloConfiguration = """
      configure<ApolloExtension> { 
        service("starwars") {
          useSemanticNaming.set(false)
          customScalarsMapping.set(mapOf("DateTime" to "java.util.Date"))
          srcDir("src/main/graphql/com/example")
          schemaFile.set(file("src/main/graphql/com/example/schema.json"))
          packageName.set("com.starwars")
          excludes.set(listOf("*.gql"))
        }
      }
    """.trimIndent()

    TestUtils.withProject(
        usesKotlinDsl = true,
        plugins = listOf(TestUtils.kotlinJvmPlugin, TestUtils.apolloPlugin),
        apolloConfiguration = apolloConfiguration
    ) { dir ->
      val result = TestUtils.executeTask("generateApolloSources", dir)
      assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)
      Assert.assertTrue(dir.generatedChild("starwars/com/starwars/DroidDetails.kt").isFile)
    }
  }
}
