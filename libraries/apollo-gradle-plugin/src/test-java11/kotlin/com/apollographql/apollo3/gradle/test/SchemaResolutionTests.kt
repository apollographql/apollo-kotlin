package com.apollographql.apollo.gradle.test


import util.TestUtils
import util.TestUtils.executeTask
import util.TestUtils.withProject
import util.generatedChild
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class SchemaResolutionTests {

  @Test
  fun `when SDL schema assert implicitly resolved and generates classes`() {
    val apolloConfiguration = """
      apollo {
        service("api") {
          packageNamesFromFilePaths()
        }
      }
    """.trimIndent()

    withProject(
        usesKotlinDsl = false,
        apolloConfiguration = apolloConfiguration,
        plugins = listOf(TestUtils.kotlinJvmPlugin, TestUtils.apolloPlugin)
    ) { projectDir ->
      File(projectDir, "src/main/graphql/com").deleteRecursively()

      val fixturesDir = TestUtils.fixturesDirectory()

      val target = File(projectDir, "src/main/graphql")
      File(fixturesDir, "sdl").copyRecursively(target = target, overwrite = true)

      File(projectDir, "src/main/graphql/schema.json").delete()

      executeTask("generateApolloSources", projectDir)

      assertTrue(projectDir.generatedChild("api/FeedRepositoryQuery.kt").isFile)
    }
  }

  @Test
  fun `when SDL schema set explicitly assert generates classes`() {
    val apolloConfiguration = """
      apollo {
        service("api") {
          packageNamesFromFilePaths()
          schemaFiles.from(file("src/main/graphql/schema.sdl"))
        }
      }
    """.trimIndent()

    withProject(
        usesKotlinDsl = false,
        apolloConfiguration = apolloConfiguration,
        plugins = listOf(TestUtils.kotlinJvmPlugin, TestUtils.apolloPlugin)
    ) { projectDir ->
      File(projectDir, "src/main/graphql/com").deleteRecursively()

      val fixturesDir = TestUtils.fixturesDirectory()

      val target = File(projectDir, "src/main/graphql")
      File(fixturesDir, "sdl").copyRecursively(target = target, overwrite = true)

      executeTask("generateApolloSources", projectDir)

      assertTrue(projectDir.generatedChild("api/FeedRepositoryQuery.kt").isFile)
    }
  }

  @Test
  fun `when both SDL and introspection schema are found build fails`() {
    val apolloConfiguration = """
      apollo {
        service("api") {
          packageNamesFromFilePaths()
        }
      }
    """.trimIndent()

    withProject(
        usesKotlinDsl = false,
        apolloConfiguration = apolloConfiguration,
        plugins = listOf(TestUtils.kotlinJvmPlugin, TestUtils.apolloPlugin)
    ) { projectDir ->
      File(projectDir, "src/main/graphql/com").deleteRecursively()

      val fixturesDir = TestUtils.fixturesDirectory()

      val target = File(projectDir, "src/main/graphql")
      File(fixturesDir, "sdl").copyRecursively(target = target, overwrite = true)

      try {
        executeTask("generateApolloSources", projectDir)
        fail("expected to fail")
      } catch (e: UnexpectedBuildFailure) {
        assertThat(e.message).contains("Multiple schemas found")
      }
    }
  }
}
