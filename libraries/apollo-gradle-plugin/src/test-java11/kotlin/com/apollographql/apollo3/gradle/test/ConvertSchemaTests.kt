package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.ast.introspection.normalize
import com.apollographql.apollo.ast.introspection.toIntrospectionSchema
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Test
import util.TestUtils
import java.io.File
import org.assertj.core.api.Assertions.assertThat as assertjThat

class ConvertSchemaTests {
  @Test
  fun `convert from SDL to Json works`() {
    TestUtils.withTestProject("convertSchema") { dir ->
      val from = "schemas/schema.sdl"
      val to = "schema.json"
      val result = TestUtils.executeTask("convertApolloSchema",
          dir,
          "--from",
          from,
          "--to",
          to
      )
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":convertApolloSchema")!!.outcome)

      /**
       * The conversion might change the order of the types
       */
      val schema1 = File(dir, to).toIntrospectionSchema().normalize()
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
      assertjThat(to.readText())
          .isEqualTo(File(dir, "schemas/schema.sdl").readText())
    }
  }

  @Test
  fun `convert from october-2015 JSON to SDL works`() {
    TestUtils.withTestProject("convertSchema") { dir ->
      // schema-october-2015.json doesn't have:
      // - `__Directive.locations` (introduced in the April2016 spec)
      // - `__Directive.isRepeatable` (introduced in the October2021 spec)
      // - `__InputField.isDeprecated` and `__InputField.deprecatedReason` (introduced after the October2021 spec)
      val from = File(dir, "schemas/schema-october-2015-spec.json")
      val to = File(dir, "schema.sdl")
      val result = TestUtils.executeTask("convertApolloSchema",
          dir,
          "--from",
          from.absolutePath,
          "--to",
          to.absolutePath
      )
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":convertApolloSchema")!!.outcome)
      assertjThat(to.readText())
          .isEqualTo(File(dir, "schemas/schema-october-2015-spec.sdl").readText())
    }
  }

}
