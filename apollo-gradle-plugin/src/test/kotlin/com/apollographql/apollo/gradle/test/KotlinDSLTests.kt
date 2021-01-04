package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.generatedChild
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.hamcrest.CoreMatchers
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
        Assert.assertThat(e.message, CoreMatchers.containsString("Unresolved reference: services"))
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
          sourceFolder.set("com/example")
          schemaFile.set(file("src/main/graphql/com/example/schema.json"))
          rootPackageName.set("com.starwars")
          exclude.set(listOf("*.gql"))
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
      Assert.assertTrue(dir.generatedChild("starwars/com/starwars/DroidDetail.kt").isFile)
    }
  }
}
