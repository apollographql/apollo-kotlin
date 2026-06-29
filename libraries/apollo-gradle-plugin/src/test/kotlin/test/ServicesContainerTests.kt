package test

import com.google.common.truth.Truth
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Assert.fail
import org.junit.Test
import util.TestUtils
import util.TestUtils.withGeneratedAccessorsProject

class ServicesContainerTests {

  @Test
  fun `apollo services collection is accessible with zero services`() {
    val apolloConfiguration = """
      println("services count: ${'$'}{apollo.services.size}")
    """.trimIndent()

    withGeneratedAccessorsProject(apolloConfiguration) { dir ->
      val message = TestUtils.executeGradle(dir).output
      Truth.assertThat(message).contains("services count: 0")
    }
  }

  @Test
  fun `apollo services collection is empty by default`() {
    val apolloConfiguration = """
      println("services empty: ${'$'}{apollo.services.isEmpty()}")
    """.trimIndent()

    withGeneratedAccessorsProject(apolloConfiguration) { dir ->
      val message = TestUtils.executeGradle(dir).output
      Truth.assertThat(message).contains("services empty: true")
    }
  }

  @Test
  fun `apollo services names can be iterated`() {
    val apolloConfiguration = """
      apollo {
        service("alpha") {
          srcDir("src/main/graphql/com/example")
          schemaFiles.from(file("src/main/graphql/com/example/schema.json"))
        }
        service("beta") {
          srcDir("src/main/graphql/com/example")
          schemaFiles.from(file("src/main/graphql/com/example/schema.json"))
        }
      }
      println("names: ${'$'}{apollo.services.map { it.name }.joinToString(",")}")
    """.trimIndent()

    withGeneratedAccessorsProject(apolloConfiguration) { dir ->
      val message = TestUtils.executeGradle(dir).output
      Truth.assertThat(message).contains("alpha")
      Truth.assertThat(message).contains("beta")
    }
  }

  @Test
  fun `apollo services can be accessed by index`() {
    val apolloConfiguration = """
      apollo {
        service("first") {
          srcDir("src/main/graphql/com/example")
          schemaFiles.from(file("src/main/graphql/com/example/schema.json"))
        }
        service("second") {
          srcDir("src/main/graphql/com/example")
          schemaFiles.from(file("src/main/graphql/com/example/schema.json"))
        }
      }
      println("first: ${'$'}{apollo.services.first().name}")
      println("second: ${'$'}{apollo.services.last().name}")
    """.trimIndent()

    withGeneratedAccessorsProject(apolloConfiguration) { dir ->
      val message = TestUtils.executeGradle(dir).output
      Truth.assertThat(message).contains("first: first")
      Truth.assertThat(message).contains("second: second")
    }
  }

  @Test
  fun `apollo services collection is read-only for adding`() {
    val apolloConfiguration = """
      val service = project.objects.newInstance(com.apollographql.apollo.gradle.internal.DefaultService::class.java, project, "starwars")
      apollo.services.add(service)
    """.trimIndent()

    withGeneratedAccessorsProject(apolloConfiguration) { dir ->
      try {
        TestUtils.executeGradle(dir)
        fail("expected to fail")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("the services collection is read-only")
      }
    }
  }

  @Test
  fun `apollo services collection is read-only for removing`() {
    val apolloConfiguration = """
      apollo {
        service("starwars") {
          srcDir("src/main/graphql/com/example")
          schemaFiles.from(file("src/main/graphql/com/example/schema.json"))
        }
      }
      apollo.services.clear()
    """.trimIndent()

    withGeneratedAccessorsProject(apolloConfiguration) { dir ->
      try {
        TestUtils.executeGradle(dir)
        fail("expected to fail")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("the services collection is read-only")
      }
    }
  }
}
