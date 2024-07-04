package com.apollographql.apollo.gradle.test

import com.google.common.truth.Truth
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Assert
import org.junit.Test
import util.TestUtils
import util.replaceInText
import java.io.File

class MultiModulesTests {
  @Test
  fun `multi-modules project compiles`() {
    TestUtils.withTestProject("multi-modules") { dir ->
      val result = TestUtils.executeTask(":leaf:assemble", dir)
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":leaf:assemble")!!.outcome)
    }
  }

  @Test
  fun `multi-modules project can use transitive dependencies`() {
    TestUtils.withTestProject("multi-modules-transitive") { dir ->
      val result = TestUtils.executeTask(":leaf:assemble", dir)
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":leaf:assemble")!!.outcome)
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":leaf:generateServiceApolloSources")!!.outcome)
    }
  }

  @Test
  fun `transitive dependencies are only included once`() {
    /**
     * A diamond shaped hierarchy does not include the schema multiple times
     */
    TestUtils.withTestProject("multi-modules-diamond") { dir ->
      val result = TestUtils.executeTask(":leaf:jar", dir)
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":leaf:generateServiceApolloSources")!!.outcome)
    }
  }

  @Test
  fun `duplicate fragments are detected correctly`() {
    TestUtils.withTestProject("multi-modules-duplicates") { dir ->
      // duplicate fragments in sibling modules are fine
      TestUtils.executeTaskAndAssertSuccess(":node1:impl:generateApolloSources", dir)

      // Now duplicate the fragment in the root
      dir.resolve("root/src/main/graphql/com/library/fragment.graphql").writeText("""
        fragment CatFragment on Cat {
          mustaches
        }
      """.trimIndent())

      try {
        // This should now fail
        TestUtils.executeTask(":node1:impl:generateApolloSources", dir)
        Assert.fail("the build did not detect duplicate fragments")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("is already defined")
      }
    }
  }

  @Test
  fun `changing a fragment in module does not recompile siblings`() {
    TestUtils.withTestProject("multi-modules-duplicates") { dir ->
      // Change the fragment so that it doesn't name clash anymore
      File(dir, "node1/impl/src/main/graphql/com/library/operations.graphql").replaceInText("CatFragment", "CatFragment1")
      // Execute jar a first time
      TestUtils.executeTaskAndAssertSuccess(":node1:impl:jar", dir)

      File(dir, "node1/impl/src/main/graphql/com/library/operations.graphql").replaceInText("CatFragment", "CatFragment2")
      val result = TestUtils.executeTask(":node1:impl:jar", dir)

      Truth.assertThat(result.task(":node1:impl:generateServiceApolloIrOperations")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
      Truth.assertThat(result.task(":node1:impl:generateServiceApolloSources")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
      Truth.assertThat(result.task(":node1:impl:compileKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

      // This is recompiled because root:generateServiceApolloSourcesFromIr needs it
      Truth.assertThat(result.task(":node2:impl:generateServiceApolloIrOperations")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
      // But the codegen and compile kotlin are not executed
      Truth.assertThat(result.task(":node2:impl:generateServiceApolloSources")?.outcome).isEqualTo(null)
      Truth.assertThat(result.task(":node2:impl:compileKotlin")?.outcome).isEqualTo(null)

      // Because we didn't add any new type, this shouldn't change
      Truth.assertThat(result.task(":root:generateServiceApolloSources")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    }
  }

  @Test
  fun `custom scalars are registered if added to customScalarMappings`() {
    TestUtils.withTestProject("multi-modules-custom-scalar") { dir ->
      TestUtils.executeTaskAndAssertSuccess(":leaf:assemble", dir)
      // Date and GeoPoint is generated in the root module
      Assert.assertTrue(File(dir, "root/build/generated/source/apollo/service/com/library/type/Date.kt").exists())
      Assert.assertTrue(File(dir, "root/build/generated/source/apollo/service/com/library/type/GeoPoint.kt").exists())
      // Leaf metadata doesn't contain Date
      Assert.assertTrue(dir.walk().filter { it.isFile && it.name == "Data.kt" }.toList().isEmpty())
    }
  }

  @Test
  fun `scalar mapping can only be registered in the schema module`() {
    TestUtils.withTestProject("multi-modules-custom-scalar-defined-in-leaf") { dir ->
      try {
        TestUtils.executeTaskAndAssertSuccess(":leaf:assemble", dir)
        Assert.fail("the build did not detect scalar mapping registered in leaf module")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("the custom scalar configuration is not used in non-schema modules")
      }
    }
  }

  @Test
  fun `metadata is published`() {
    TestUtils.withTestProject("multi-modules-publishing-producer") { dir ->
      TestUtils.executeTaskAndAssertSuccess(
          ":publishAllPublicationsToPluginTestRepository",
          dir
      )
    }
    TestUtils.withTestProject("multi-modules-publishing-consumer") { dir ->
      TestUtils.executeTaskAndAssertSuccess(
          ":build",
          dir
      )
    }
  }

  @Test
  fun `schema targetLanguage propagates`() {
    TestUtils.withTestProject("multi-modules-badconfig") { dir ->
      dir.resolve("root/build.gradle.kts").replacePlaceHolder("generateKotlinModels.set(false)")
      TestUtils.executeTaskAndAssertSuccess(":leaf:build", dir)
    }
  }

  @Test
  fun `schema codegenModels propagates`() {
    TestUtils.withTestProject("multi-modules-badconfig") { dir ->
      dir.resolve("root/build.gradle.kts").replacePlaceHolder("codegenModels.set(\"responseBased\")")
      TestUtils.executeTaskAndAssertSuccess(":leaf:build", dir)
    }
  }

  @Test
  fun `bad targetLanguage is detected`() {
    TestUtils.withTestProject("multi-modules-badconfig") { dir ->
      dir.resolve("root/build.gradle.kts").replacePlaceHolder("generateKotlinModels.set(true)")
      dir.resolve("leaf/build.gradle.kts").replacePlaceHolder("generateKotlinModels.set(false)")

      try {
        TestUtils.executeTask(":leaf:generateServiceApolloOptions", dir)
        Assert.fail("the build did not detect the bad target language")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("Check your generateKotlinModels settings")
      }
    }
  }

  @Test
  fun `bad codegenModels is detected`() {
    TestUtils.withTestProject("multi-modules-badconfig") { dir ->
      dir.resolve("root/build.gradle.kts").replacePlaceHolder("codegenModels.set(\"responseBased\")")
      dir.resolve("leaf/build.gradle.kts").replacePlaceHolder("codegenModels.set(\"operationBased\")")

      try {
        TestUtils.executeTask(":leaf:generateServiceApolloOptions", dir)
        Assert.fail("the build did not detect the bad target codegenModels")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("Check your codegenModels setting")
      }
    }
  }
}

private fun File.replacePlaceHolder(replacement: String) = replaceInText("// PLACEHOLDER".shr(4), replacement.shr(4))

internal fun String.shr(c: Int): String {
  return split("\n").map { padStart(c, ' ') }.joinToString("\n")
}
