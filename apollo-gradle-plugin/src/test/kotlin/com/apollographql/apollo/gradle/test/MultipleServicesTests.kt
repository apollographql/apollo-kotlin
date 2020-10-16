package com.apollographql.apollo.gradle.test


import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.TestUtils.withProject
import com.apollographql.apollo.gradle.util.generatedChild
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class MultipleServicesTests {
  fun withMultipleServicesProject(apolloConfiguration: String, block: (File) -> Unit) {
    withProject(usesKotlinDsl = false,
        plugins = listOf(TestUtils.kotlinJvmPlugin, TestUtils.apolloPlugin),
        apolloConfiguration = apolloConfiguration) { dir ->
      val source = TestUtils.fixturesDirectory()

      val target = File(dir, "src/main/graphql/githunt")
      File(source, "githunt").copyRecursively(target = target, overwrite = true)

      File(dir, "src/main/graphql/com").copyRecursively(target = File(dir, "src/main/graphql/starwars"), overwrite = true)
      File(dir, "src/main/graphql/com").deleteRecursively()

      block(dir)
    }
  }

  @Test
  fun `multiple schema files without using service is not supported`() {
    withMultipleServicesProject("") { dir ->
      try {
        TestUtils.executeTask("generateApolloSources", dir)
        fail("expected to fail")
      } catch (e: UnexpectedBuildFailure) {
        MatcherAssert.assertThat(
            e.message,
            containsString("Multiple schemas found")
        )
      }
    }
  }

  @Test
  fun `can specify services explicitly`() {
    val apolloConfiguration = """
      apollo {
        service("starwars") {
          sourceFolder = "starwars"
        }
        service("githunt") {
          sourceFolder = "githunt"
        }
      }
    """.trimIndent()
    withMultipleServicesProject(apolloConfiguration) { dir ->
      TestUtils.executeTask("build", dir)

      assertTrue(dir.generatedChild("starwars/example/DroidDetailsQuery.kt").isFile)
      assertTrue(dir.generatedChild("starwars/example/FilmsQuery.kt").isFile)
      assertTrue(dir.generatedChild("starwars/example/fragment/SpeciesInformation.kt").isFile)
      assertTrue(dir.generatedChild("githunt/FeedQuery.kt").isFile)
      assertTrue(dir.generatedChild("githunt/fragment/RepositoryFragment.kt").isFile)
    }
  }
}
