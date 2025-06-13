package test


import util.TestUtils
import util.TestUtils.withProject
import util.generatedSource
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class MultiServicesTests {
  private fun withMultipleServicesProject(apolloConfiguration: String, block: (File) -> Unit) {
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
  fun `multiple schema files in different folders throw an error`() {
    val apolloConfiguration = """
      apollo {
        service("service") {
          packageNamesFromFilePaths()
        }
      }
    """.trimIndent()
    withMultipleServicesProject(apolloConfiguration) { dir ->
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
  fun executableSchemaFails() {
    TestUtils.withTestProject("executable-schema-file") { dir ->
      try {
        TestUtils.executeTask("generateApolloSources", dir)
        fail("expected to fail")
      } catch (e: UnexpectedBuildFailure) {
        MatcherAssert.assertThat(
            e.message,
            containsString("But none of them contain type definitions.")
        )
      }
    }
  }

  @Test
  fun `can specify services explicitly`() {
    val apolloConfiguration = """
      apollo {
        service("starwars") {
          packageName.set("starwars")
          srcDir("src/main/graphql/starwars")
        }
        service("githunt") {
          packageName.set("githunt")
          srcDir("src/main/graphql/githunt")
        }
      }
    """.trimIndent()
    withMultipleServicesProject(apolloConfiguration) { dir ->
      TestUtils.executeTask("build", dir)

      assertTrue(dir.generatedSource("starwars/DroidDetailsQuery.kt", "starwars").isFile)
      assertTrue(dir.generatedSource("starwars/FilmsQuery.kt", "starwars").isFile)
      assertTrue(dir.generatedSource("starwars/fragment/SpeciesInformation.kt", "starwars").isFile)
      assertTrue(dir.generatedSource("githunt/FeedQuery.kt", "githunt").isFile)
      assertTrue(dir.generatedSource("githunt/fragment/RepositoryFragment.kt", "githunt").isFile)
    }
  }
}
