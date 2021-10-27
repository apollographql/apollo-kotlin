package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.gradle.util.TestUtils
import com.google.common.truth.Truth
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Assert
import org.junit.Test

class LanguageVersionTests {
  @Test
  fun `compiling with compatible languageVersion is working`() {
    val configuration = getConfiguration(kotlinLanguageVersion = "1.5", apolloLanguageVersion = "1.5.0")
    TestUtils.withProject(
        usesKotlinDsl = true,
        plugins = listOf(TestUtils.kotlinJvmPlugin, TestUtils.apolloPlugin),
        apolloConfiguration = configuration,
        graphqlPath = "hero",
    ) { dir ->
      TestUtils.executeTaskAndAssertSuccess(":assemble", dir)
    }
  }

  @Test
  fun `compiling with incompatible languageVersion is not working`() {
    val configuration = getConfiguration(kotlinLanguageVersion = "1.4", apolloLanguageVersion = "1.5.0")
    TestUtils.withProject(
        usesKotlinDsl = true,
        plugins = listOf(TestUtils.kotlinJvmPlugin, TestUtils.apolloPlugin),
        apolloConfiguration = configuration,
        graphqlPath = "hero",
    ) { dir ->
      try {
        TestUtils.executeTask(":assemble", dir)
        Assert.fail("Compiling with incompatible languageVersion should fail")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("The feature \"sealed interfaces\" is only available since language version 1.5")
      }
    }
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
