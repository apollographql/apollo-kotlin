package test

import com.google.common.truth.Truth
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test
import util.TestUtils
import util.TestUtils.setVersionsUnderTest
import util.agp8_kgp2_0
import util.generatedSource
import java.io.File
import kotlin.test.assertEquals

class LanguageVersionTests {
  private val versionsUnderTest = agp8_kgp2_0
  private val gradleVersion = versionsUnderTest.gradle

  @Test
  fun `compiling with 1_5 features with Kotlin 1_5 is working`() {
    withProject(kotlinLanguageVersion = "1.5", apolloLanguageVersion = "1.5") { dir ->
      TestUtils.executeGradleWithVersion(dir, gradleVersion, ":assemble").apply {
        assertEquals(TaskOutcome.SUCCESS, task(":assemble")!!.outcome)
      }
    }
  }

  @Test
  fun `compiling with 1_5 features with Kotlin 1_4 is not working`() {
    withProject(kotlinLanguageVersion = "1.4", apolloLanguageVersion = "1.5") { dir ->
      try {
        TestUtils.executeGradleWithVersion(dir, gradleVersion, ":assemble")
        Assert.fail("Compiling with incompatible languageVersion should fail")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("The feature \"sealed interfaces\" is only available since language version 1.5")
      }
    }
  }

  @Test
  fun `using bogus languageVersion fails`() {
    withProject(kotlinLanguageVersion = "1.5", apolloLanguageVersion = "3.14") { dir ->
      try {
        TestUtils.executeGradleWithVersion(dir, gradleVersion, ":assemble")
        Assert.fail("Compiling with incompatible languageVersion should fail")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("languageVersion '3.14' is not supported")
      }
    }
  }

  @Test
  fun `compiling with 1_9 features generates entries in enums`() {
    withProject(apolloLanguageVersion = "1.9", graphqlPath = "githunt") { dir ->

      TestUtils.executeGradleWithVersion(dir, gradleVersion, ":generateApolloSources").apply {
        assertEquals(TaskOutcome.SUCCESS, task(":generateApolloSources")!!.outcome)
      }
      assertTrue(dir.generatedSource("com/example/type/FeedType.kt").readText().contains("entries.find"))
    }
  }

  @Test
  fun `compiling with 1_5 features generates values in enums`() {
    withProject(apolloLanguageVersion = "1.5", graphqlPath = "githunt") { dir ->

      TestUtils.executeGradleWithVersion(dir, gradleVersion, ":generateApolloSources").apply {
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
      setVersionsUnderTest(dir, versionsUnderTest)
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
        kotlinOptions {
          ${if (kotlinLanguageVersion == null) "" else "languageVersion = \"$kotlinLanguageVersion\""}
          ${if (kotlinApiVersion == null) "" else "apiVersion = \"$kotlinApiVersion\""}
        }
      }      
      """.trimIndent()

    val apolloConfiguration = """
      apollo {
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
