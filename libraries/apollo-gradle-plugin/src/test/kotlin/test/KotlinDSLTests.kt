package test

import util.TestUtils
import util.generatedSource
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
        Truth.assertThat(e.message).contains("Unresolved reference 'services'.")
      }
      Assert.assertNotNull(exception)
    }
  }

  @Test
  fun `parameters do not throw`() {
    val apolloConfiguration = """
      apollo { 
        service("service") {
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
      Assert.assertTrue(dir.generatedSource("com/starwars/DroidDetails.kt").isFile)
    }
  }

  @Test
  fun `nested service block cannot access outer dsl scope implicitly`() {
    val apolloConfiguration = """
      apollo {
        service("service1") {
          service("service2") {
          }
        }
      }
    """.trimIndent()

    TestUtils.withGeneratedAccessorsProject(apolloConfiguration) { dir ->
      var exception: Exception? = null
      try {
        TestUtils.executeGradle(dir)
      } catch (e: UnexpectedBuildFailure) {
        exception = e
        Truth.assertThat(e.message).contains("implicit receiver")
      }
      Assert.assertNotNull(exception)
    }
  }

  @Test
  fun `nested service block can access outer dsl scope with explicit receiver`() {
    val apolloConfiguration = """
      apollo {
        service("service1") {
          this@apollo.service("service2") {
          }
        }
      }
    """.trimIndent()

    TestUtils.withGeneratedAccessorsProject(apolloConfiguration) { dir ->
      TestUtils.executeGradle(dir)
    }
  }
}
