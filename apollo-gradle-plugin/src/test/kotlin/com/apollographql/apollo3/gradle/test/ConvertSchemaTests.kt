package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.compiler.introspection.normalize
import com.apollographql.apollo3.compiler.introspection.toIntrospectionSchema
import com.apollographql.apollo3.gradle.util.TestUtils
import com.google.common.truth.Truth
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Test
import java.io.File
import org.assertj.core.api.Assertions.assertThat as assertjThat

class ConvertSchemaTests {
  @Test
  fun `convert from SDL to Json works`() {
    TestUtils.withTestProject("convertSchema") { dir ->
      val from = File(dir, "schemas/schema.sdl")
      val to = File(dir, "schema.json")
      val result = TestUtils.executeTask("convertApolloSchema",
          dir,
          "--from",
          from.absolutePath,
          "--to",
          to.absolutePath
      )
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":convertApolloSchema")!!.outcome)

      /**
       * The conversion might change the order of the types
       */
      val schema1 = to.toIntrospectionSchema().normalize()
      val schema2 = File(dir, "schemas/schema.json").toIntrospectionSchema().normalize()
      assertjThat(schema1)
          .usingRecursiveComparison()
          .isEqualTo(schema2)
    }
  }

  @Test
  fun `convert is never up-to-date`() {
    TestUtils.withTestProject("convertSchema") { dir ->
      val from = File(dir, "schemas/schema.sdl")
      val to = File(dir, "schema.json")
      var result = TestUtils.executeTask("convertApolloSchema",
          dir,
          "--from",
          from.absolutePath,
          "--to",
          to.absolutePath
      )
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":convertApolloSchema")!!.outcome)
      result = TestUtils.executeTask("convertApolloSchema",
          dir,
          "--from",
          from.absolutePath,
          "--to",
          to.absolutePath
      )
      // even if inputs are the same,
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":convertApolloSchema")!!.outcome)
    }
  }

  @Test
  fun `convert from Json to SDL works`() {
    TestUtils.withTestProject("convertSchema") { dir ->
      val from = File(dir, "schemas/schema.json")
      val to = File(dir, "schema.sdl")
      val result = TestUtils.executeTask("convertApolloSchema",
          dir,
          "--from",
          from.absolutePath,
          "--to",
          to.absolutePath
      )
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":convertApolloSchema")!!.outcome)
      // SDL to Json and back is not idempotent as SDL will add a `schema {}` block
      Truth.assertThat(to.readText()).contains("""
        type Query {
          greeting: String
        }
      """.trimIndent())
    }
  }
}