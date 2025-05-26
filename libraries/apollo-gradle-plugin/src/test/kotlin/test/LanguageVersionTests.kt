package test

import com.google.common.truth.Truth
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test
import util.TestUtils
import util.disableIsolatedProjects
import util.generatedSource
import java.io.File
import kotlin.test.assertEquals

class LanguageVersionTests {
  @Test
  fun `compiling with 1_5 features with Kotlin 1_5 is working`() {
    withProject(kotlinLanguageVersion = "org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_5", apolloLanguageVersion = "1.5") { dir ->
      dir.disableIsolatedProjects() // old KGP versions do not support isolated projects
      TestUtils.executeGradleWithVersion(dir, "8.10", ":assemble").apply {
        assertEquals(TaskOutcome.SUCCESS, task(":assemble")!!.outcome)
      }
    }
  }

  @Test
  fun `compiling with 1_5 features with Kotlin 1_4 is not working`() {
    withProject(kotlinLanguageVersion = "org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_4", apolloLanguageVersion = "1.5") { dir ->
      try {
        TestUtils.executeGradleWithVersion(dir, "8.10", ":assemble")
        Assert.fail("Compiling with incompatible languageVersion should fail")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("The feature \"sealed interfaces\" is only available since language version 1.5")
      }
    }
  }

  @Test
  fun `using bogus languageVersion fails`() {
    withProject(kotlinLanguageVersion = "org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_5", apolloLanguageVersion = "3.14") { dir ->
      try {
        TestUtils.executeGradleWithVersion(dir, "8.10", ":assemble")
        Assert.fail("Compiling with incompatible languageVersion should fail")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("languageVersion '3.14' is not supported")
      }
    }
  }

  @Test
  fun `compiling with 1_9 features generates entries in enums`() {
    withProject(apolloLanguageVersion = "1.9", graphqlPath = "githunt") { dir ->

      TestUtils.executeGradleWithVersion(dir, "8.10", ":generateApolloSources").apply {
        assertEquals(TaskOutcome.SUCCESS, task(":generateApolloSources")!!.outcome)
      }
      assertTrue(dir.generatedSource("com/example/type/FeedType.kt").readText().contains("entries.find"))
    }
  }

  @Test
  fun `compiling with 1_5 features generates values in enums`() {
    withProject(apolloLanguageVersion = "1.5", graphqlPath = "githunt") { dir ->

      TestUtils.executeGradleWithVersion(dir, "8.10", ":generateApolloSources").apply {
        assertEquals(TaskOutcome.SUCCESS, task(":generateApolloSources")!!.outcome)
      }
      assertTrue(dir.generatedSource("com/example/type/FeedType.kt").readText().contains("values().find"))
    }
  }

  private fun withProject(
      kotlinLanguageVersion: String? = null,
      kotlinApiVersion: String? = null,
      apolloLanguageVersion: String? = null,
      graphqlPath: String = "hero",
      block: (File) -> Unit,
  ) {
    TestUtils.withTestProject("language-version") { dir ->
      dir.resolve("build.gradle.kts").appendText(
          getConfiguration(
              kotlinLanguageVersion = kotlinLanguageVersion,
              kotlinApiVersion = kotlinApiVersion,
              apolloLanguageVersion = apolloLanguageVersion,
          )
      )

      TestUtils.fixturesDirectory().resolve(graphqlPath).copyRecursively(dir.resolve("src/main/graphql/com/example"))

      block(dir)
    }
  }

  private fun getConfiguration(
      kotlinLanguageVersion: String?,
      kotlinApiVersion: String?,
      apolloLanguageVersion: String?,
  ): String {
    val kotlinConfiguration = """
      tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
          ${if (kotlinLanguageVersion == null) "" else "languageVersion.set($kotlinLanguageVersion)"}
          ${if (kotlinApiVersion == null) "" else "apiVersion.set(\"$kotlinApiVersion\")"}
        }
      }      
      """.trimIndent()

    val apolloConfiguration = """
      configure<ApolloExtension> {
        service("service") {
          packageNamesFromFilePaths()
          codegenModels.set("responseBased")
          ${if (apolloLanguageVersion == null) "" else "languageVersion.set(\"$apolloLanguageVersion\")"}
        }
      }
      """.trimIndent()
    return kotlinConfiguration + "\n" + apolloConfiguration
  }
}
