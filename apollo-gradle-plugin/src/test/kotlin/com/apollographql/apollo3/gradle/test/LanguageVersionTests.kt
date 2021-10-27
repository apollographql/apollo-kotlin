package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.gradle.util.TestUtils
import com.google.common.truth.Truth
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Assert
import org.junit.Test
import java.io.File

class LanguageVersionTests {
  @Test
  fun `compiling with 1_4 features with Kotlin 1_4 is working`() {
    withProject(kotlinLanguageVersion = "1.4", apolloLanguageVersion = "1.4") { dir ->
      TestUtils.executeTaskAndAssertSuccess(":assemble", dir)
    }
  }

  @Test
  fun `compiling with 1_5 features with Kotlin 1_5 is working`() {
    withProject(kotlinLanguageVersion = "1.5", apolloLanguageVersion = "1.5") { dir ->
      TestUtils.executeTaskAndAssertSuccess(":assemble", dir)
    }
  }

  @Test
  fun `compiling with 1_5 features with Kotlin 1_4 is not working`() {
    withProject(kotlinLanguageVersion = "1.4", apolloLanguageVersion = "1.5") { dir ->
      try {
        TestUtils.executeTask(":assemble", dir)
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
        TestUtils.executeTask(":assemble", dir)
        Assert.fail("Compiling with incompatible languageVersion should fail")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("languageVersion '3.14' is not supported")
      }
    }
  }

  private fun withProject(
      kotlinLanguageVersion: String,
      apolloLanguageVersion: String,
      block: (File) -> Unit,
  ) {
    TestUtils.withProject(
        usesKotlinDsl = true,
        plugins = listOf(TestUtils.kotlinJvmPlugin, TestUtils.apolloPlugin),
        apolloConfiguration = getConfiguration(kotlinLanguageVersion = kotlinLanguageVersion, apolloLanguageVersion = apolloLanguageVersion),
        graphqlPath = "hero",
        block = block
    )
  }

  private fun getConfiguration(kotlinLanguageVersion: String, apolloLanguageVersion: String): String {
    val kotlinConfiguration = """
      tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
          languageVersion = "$kotlinLanguageVersion"
        }
      }      
      """.trimIndent()

    val apolloConfiguration = """
      configure<ApolloExtension> {
        packageNamesFromFilePaths()
        codegenModels.set(com.apollographql.apollo3.compiler.MODELS_RESPONSE_BASED)
        languageVersion.set("$apolloLanguageVersion")
      }
      """.trimIndent()
    return kotlinConfiguration + "\n" + apolloConfiguration
  }
}
