package com.apollographql.apollo.gradle.test

import com.google.common.truth.Truth
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Assert
import org.junit.Test
import util.TestUtils
import java.io.File

class LanguageVersionTests {
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

  @Test
  fun `compiling with 1_9 features generates entries in enums`() {
    withProject(apolloLanguageVersion = "1.9", graphqlPath = "githunt") { dir ->
      TestUtils.executeTaskAndAssertSuccess(":generateApolloSources", dir)
      Assert.assertTrue(File(dir, "build/generated/source/apollo/service/com/example/type/FeedType.kt").readText().contains("entries.find"))
    }
  }

  @Test
  fun `compiling with 1_5 features generates values in enums`() {
    withProject(apolloLanguageVersion = "1.5", graphqlPath = "githunt") { dir ->
      TestUtils.executeTaskAndAssertSuccess(":generateApolloSources", dir)
      Assert.assertTrue(File(dir, "build/generated/source/apollo/service/com/example/type/FeedType.kt").readText().contains("values().find"))
    }
  }

  private fun withProject(
      kotlinLanguageVersion: String? = null,
      kotlinApiVersion: String? = null,
      apolloLanguageVersion: String? = null,
      graphqlPath: String = "hero",
      block: (File) -> Unit,
  ) {
    TestUtils.withProject(
        usesKotlinDsl = true,
        plugins = listOf(TestUtils.kotlinJvmPlugin, TestUtils.apolloPlugin),
        apolloConfiguration = getConfiguration(
            kotlinLanguageVersion = kotlinLanguageVersion,
            kotlinApiVersion = kotlinApiVersion,
            apolloLanguageVersion = apolloLanguageVersion,
        ),
        graphqlPath = graphqlPath,
        block = block
    )
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
          codegenModels.set(com.apollographql.apollo.compiler.MODELS_RESPONSE_BASED)
          ${if (apolloLanguageVersion == null) "" else "languageVersion.set(\"$apolloLanguageVersion\")"}
        }
      }
      """.trimIndent()
    return kotlinConfiguration + "\n" + apolloConfiguration
  }
}
