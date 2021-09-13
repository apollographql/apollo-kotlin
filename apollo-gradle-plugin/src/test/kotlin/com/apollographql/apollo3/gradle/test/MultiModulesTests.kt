package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.compiler.ApolloMetadata
import com.apollographql.apollo3.gradle.util.TestUtils
import com.apollographql.apollo3.gradle.util.replaceInText
import com.google.common.truth.Truth
import junit.framework.Assert.assertTrue
import junit.framework.Assert.fail
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
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":leaf:generateServiceApolloSources")!!.outcome)
    }
  }

  @Test
  fun `transitive dependencies are only included once`() {
    /**
     * A dimaond shaped hierarchy does not include the schema multiple times
     */
    TestUtils.withTestProject("multi-modules-diamond") { dir ->
      val result = TestUtils.executeTask(":leaf:jar", dir)
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":leaf:generateServiceApolloSources")!!.outcome)
    }
  }

  @Test
  fun `duplicate fragments are detected correctly`() {
    TestUtils.withTestProject("multi-modules-duplicates") { dir ->
      try {
        TestUtils.executeTask(":node1:generateApolloSources", dir)
        fail("the build did not detect duplicate classes")
      } catch(e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("duplicate")
        Truth.assertThat(e.message).contains("in modules: node1,node2")
      }
    }
  }

  @Test
  fun `changing a module does not recompile siblings`() {
    TestUtils.withTestProject("multi-modules-duplicates") { dir ->
      File(dir, "node1/src/main/graphql/com/library/operations.graphql").replaceInText("CatFragment", "CatFragment1")
      TestUtils.executeTaskAndAssertSuccess(":node1:generateApolloSources", dir)
      File(dir, "node1/src/main/graphql/com/library/operations.graphql").replaceInText("CatFragment", "CatFragment2")
      val result = TestUtils.executeTask(":node1:jar", dir)

      Truth.assertThat(result.task(":node1:generateServiceApolloSources")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
      Truth.assertThat(result.task(":node1:compileKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
      Truth.assertThat(result.task(":checkServiceApolloDuplicates")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
      Truth.assertThat(result.task(":node2:compileKotlin")?.outcome).isEqualTo(null)
      Truth.assertThat(result.task(":node2:generateServiceApolloSources")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    }
  }

  @Test
  fun `custom scalars are registered if added to customScalarMappings`() {
    TestUtils.withTestProject("multi-modules-custom-scalar") { dir ->
      TestUtils.executeTaskAndAssertSuccess(":leaf:assemble", dir)
      // Date is generated in the root module
      assertTrue(File(dir, "root/build/generated/source/apollo/service/com/library/type/Date.kt").exists())
      // Leaf metadata doesn't contain anything regarding Date
      val metadata = ApolloMetadata.readFrom(File(dir, "leaf/build/generated/metadata/apollo/service/metadata.json"))
      Truth.assertThat(metadata.compilerMetadata.resolverInfo.entries.map {it.key.id}).doesNotContain("Date")
      // But contains GeoPoint
      Truth.assertThat(metadata.compilerMetadata.resolverInfo.entries.map {it.key.id}).doesNotContain("Date")
      assertTrue(File(dir, "leaf/build/generated/source/apollo/service/com/library/type/GeoPoint.kt").exists())
    }
  }
}