package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.util.TestUtils
import com.google.common.truth.Truth
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Test
import java.io.File

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
      Truth.assertThat(to.readText()).isEqualTo(File(dir, "schemas/schema.json").readText())
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