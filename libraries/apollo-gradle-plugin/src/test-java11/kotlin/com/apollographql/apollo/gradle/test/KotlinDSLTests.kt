package com.apollographql.apollo.gradle.test

import util.TestUtils
import util.generatedChild
import com.google.common.truth.Truth
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class KotlinDSLTests {
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
      apollo { 
        service("starwars") {
          useSemanticNaming.set(false)
          mapScalar("DateTime", "java.util.Date")
          srcDir("src/main/graphql/com/example")
          schemaFiles.from(file("src/main/graphql/com/example/schema.json"))
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
