package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.internal.child
import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.TestUtils.executeTask
import com.apollographql.apollo.gradle.util.TestUtils.withProject
import com.apollographql.apollo.gradle.util.generatedChild
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SchemaResolutionTests {

  @Test
  fun `when SDL schema assert implicitly resolved and generates classes`() {
    val apolloConfiguration = """
      apollo {
        service("api") {
        }
      }
    """.trimIndent()

    withProject(
        usesKotlinDsl = false,
        apolloConfiguration = apolloConfiguration,
        plugins = listOf(TestUtils.kotlinJvmPlugin, TestUtils.apolloPlugin)
    ) { projectDir ->
      projectDir.child("src", "main", "graphql", "com").deleteRecursively()

      val fixturesDir = TestUtils.fixturesDirectory()

      val target = projectDir.child("src", "main", "graphql")
      fixturesDir.child("sdl").copyRecursively(target = target, overwrite = true)

      projectDir.child("src", "main", "graphql", "schema.json").delete()

      executeTask("generateApolloSources", projectDir)

      assertTrue(projectDir.generatedChild("main/api/FeedRepositoryQuery.kt").isFile)
    }
  }

  @Test
  fun `when SDL schema set explicitly assert generates classes`() {
    val apolloConfiguration = """
      apollo {
        service("api") {
          schemaPath = "schema.sdl"
        }
      }
    """.trimIndent()

    withProject(
        usesKotlinDsl = false,
        apolloConfiguration = apolloConfiguration,
        plugins = listOf(TestUtils.kotlinJvmPlugin, TestUtils.apolloPlugin)
    ) { projectDir ->
      projectDir.child("src", "main", "graphql", "com").deleteRecursively()

      val fixturesDir = TestUtils.fixturesDirectory()

      val target = projectDir.child("src", "main", "graphql")
      fixturesDir.child("sdl").copyRecursively(target = target, overwrite = true)

      executeTask("generateApolloSources", projectDir)

      assertTrue(projectDir.generatedChild("main/api/FeedRepositoryQuery.kt").isFile)
    }
  }

  @Test
  fun `when SDL and introspection schema not set explicitly assert build fails`() {
    val apolloConfiguration = """
      apollo {
        service("api") {
        }
      }
    """.trimIndent()

    withProject(
        usesKotlinDsl = false,
        apolloConfiguration = apolloConfiguration,
        plugins = listOf(TestUtils.kotlinJvmPlugin, TestUtils.apolloPlugin)
    ) { projectDir ->
      projectDir.child("src", "main", "graphql", "com").deleteRecursively()

      val fixturesDir = TestUtils.fixturesDirectory()

      val target = projectDir.child("src", "main", "graphql")
      fixturesDir.child("sdl").copyRecursively(target = target, overwrite = true)

      try {
        executeTask("generateApolloSources", projectDir)
        fail("expected to fail")
      } catch (e: UnexpectedBuildFailure) {
        MatcherAssert.assertThat(
            e.message,
            CoreMatchers.containsString("ApolloGraphQL: By default only one schema.[json | sdl] file is supported.")
        )
      }
    }
  }
}
