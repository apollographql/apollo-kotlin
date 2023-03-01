package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.compiler.schemaTypes
import com.apollographql.apollo3.compiler.toCodegenMetadata
import com.apollographql.apollo3.gradle.util.TestUtils
import com.apollographql.apollo3.gradle.util.replaceInText
import com.google.common.truth.Truth
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Assert
import org.junit.Test
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
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":leaf:generateServiceApolloSourcesFromIr")!!.outcome)
    }
  }

  @Test
  fun `transitive dependencies are only included once`() {
    /**
     * A diamond shaped hierarchy does not include the schema multiple times
     */
    TestUtils.withTestProject("multi-modules-diamond") { dir ->
      val result = TestUtils.executeTask(":leaf:jar", dir)
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":leaf:generateServiceApolloSourcesFromIr")!!.outcome)
    }
  }

  @Test
  fun `duplicate fragments are detected correctly`() {
    TestUtils.withTestProject("multi-modules-duplicates") { dir ->
      try {
        TestUtils.executeTask(":node1:impl:generateApolloSources", dir)
        Assert.fail("the build did not detect duplicate classes")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("duplicate fragments")
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

      Truth.assertThat(result.task(":node1:impl:generateServiceApolloIr")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
      Truth.assertThat(result.task(":node1:impl:generateServiceApolloSourcesFromIr")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
      Truth.assertThat(result.task(":node1:impl:compileKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

      // This is recompiled because root:generateServiceApolloSourcesFromIr needs it
      Truth.assertThat(result.task(":node2:impl:generateServiceApolloIr")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
      // But the codegen and compile kotlin are not executed
      Truth.assertThat(result.task(":node2:impl:generateServiceApolloSourcesFromIr")?.outcome).isEqualTo(null)
      Truth.assertThat(result.task(":node2:impl:compileKotlin")?.outcome).isEqualTo(null)

      // Because we didn't add any new type, this shouldn't change
      Truth.assertThat(result.task(":root:generateServiceApolloSourcesFromIr")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    }
  }

  @Test
  fun `custom scalars are registered if added to customScalarMappings`() {
    TestUtils.withTestProject("multi-modules-custom-scalar") { dir ->
      TestUtils.executeTaskAndAssertSuccess(":leaf:assemble", dir)
      // Date and GeoPoint is generated in the root module
      Assert.assertTrue(File(dir, "root/build/generated/source/apollo/service/com/library/type/Date.kt").exists())
      Assert.assertTrue(File(dir, "root/build/generated/source/apollo/service/com/library/type/GeoPoint.kt").exists())
      // Leaf metadata doesn't contain anything regarding Date
      val codegenMetadata = File(dir, "leaf/build/generated/metadata/apollo/service/metadata.json").toCodegenMetadata()
      Truth.assertThat(codegenMetadata.schemaTypes()).doesNotContain("Date")
    }
  }

  @Test
  fun `scalar mapping can only be registered in the schema module`() {
    TestUtils.withTestProject("multi-modules-custom-scalar-defined-in-leaf") { dir ->
      try {
        TestUtils.executeTaskAndAssertSuccess(":leaf:assemble", dir)
        Assert.fail("the build did not detect scalar mapping registered in leaf module")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("scalarTypeMapping is not used because this module depends on another one that has already set scalarTypeMapping")
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
}
