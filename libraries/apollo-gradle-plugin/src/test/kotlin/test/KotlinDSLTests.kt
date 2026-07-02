package test

import util.TestUtils
import util.generatedSource
import com.google.common.truth.Truth
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class KotlinDSLTests {
  @Test
  fun `DefaultApolloExtension should expose a NamedDomainObjectCollection`() {
    val apolloConfiguration = """
      apollo {
        service("service") {
          srcDir("src/main/graphql/com/example")
          schemaFiles.from(file("src/main/graphql/com/example/schema.json"))
        }
      }

      println("apollo has ${'$'}{apollo.services.size} services")
    """.trimIndent()

    TestUtils.withGeneratedAccessorsProject(apolloConfiguration) {dir ->
      val message = TestUtils.executeGradle(dir).output
      Truth.assertThat(message).contains("apollo has 1 services")
    }
  }

  @Test
  fun `services can be accessed by name via Named interface`() {
    val apolloConfiguration = """
      apollo {
        service("starwars") {
          srcDir("src/main/graphql/com/example")
          schemaFiles.from(file("src/main/graphql/com/example/schema.json"))
        }
      }

      println("service schemaFiles count is ${'$'}{apollo.services.named("starwars").get().schemaFiles.count()}")
    """.trimIndent()

    TestUtils.withGeneratedAccessorsProject(apolloConfiguration) {dir ->
      val message = TestUtils.executeGradle(dir).output
      Truth.assertThat(message).contains("service schemaFiles count is 1")
    }
  }

  @Test
  fun `services collection reflects multiple registered services`() {
    val apolloConfiguration = """
      apollo {
        service("starwars") {
          srcDir("src/main/graphql/com/example")
          schemaFiles.from(file("src/main/graphql/com/example/schema.json"))
        }
        service("githunt") {
          srcDir("src/main/graphql/com/example")
          schemaFiles.from(file("src/main/graphql/com/example/schema.json"))
        }
      }

      println("apollo has ${'$'}{apollo.services.size} services")
      println("names: ${'$'}{apollo.services.names}")
    """.trimIndent()

    TestUtils.withGeneratedAccessorsProject(apolloConfiguration) {dir ->
      val message = TestUtils.executeGradle(dir).output
      Truth.assertThat(message).contains("apollo has 2 services")
      Truth.assertThat(message).contains("names: [githunt, starwars]")
    }
  }

  @Test
  fun `service name is accessible through Named interface after registration`() {
    val apolloConfiguration = """
      apollo {
        service("starwars") {
          srcDir("src/main/graphql/com/example")
          schemaFiles.from(file("src/main/graphql/com/example/schema.json"))
        }
      }
      println("name: ${'$'}{apollo.services.named("starwars").get().name}")
    """.trimIndent()

    TestUtils.withGeneratedAccessorsProject(apolloConfiguration) {dir ->
      val message = TestUtils.executeGradle(dir).output
      Truth.assertThat(message).contains("name: starwars")
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
}
