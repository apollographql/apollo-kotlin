package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.parser.error.ParseException
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema.Companion.wrap
import com.apollographql.apollo.compiler.parser.introspection.toSDL
import com.apollographql.apollo.compiler.parser.sdl.GraphSdlSchema
import com.apollographql.apollo.compiler.parser.sdl.toIntrospectionSchema
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class GraphSdlParseTest() {

  /**
   * GraphQL has Int and Float, json has only Number, map everything to Double
   */
  private fun Any?.normalizeNumbers(): Any? {
    return when (this) {
      is List<*> -> this.map { it?.normalizeNumbers() }
      is Number -> this.toDouble()
      else -> this
    }
  }

  private fun IntrospectionSchema.Type.normalize(): IntrospectionSchema.Type {
    return when (this) {
      is IntrospectionSchema.Type.Scalar -> this
      is IntrospectionSchema.Type.Object -> copy(fields = fields?.sortedBy { field -> field.name })
      is IntrospectionSchema.Type.Interface -> copy(fields = fields?.sortedBy { field -> field.name })
      is IntrospectionSchema.Type.Union -> copy(fields = fields?.sortedBy { field -> field.name })
      is IntrospectionSchema.Type.Enum -> this
      is IntrospectionSchema.Type.InputObject -> copy(inputFields = inputFields.map {
        it.copy(defaultValue = it.defaultValue.normalizeNumbers())
      }.sortedBy { field -> field.name })
    }
  }

  @Test
  fun `SDL schema parsed successfully and produced the same introspection schema`() {
    /**
     * Things to watch out:
     * - leading/trailing spaces in descriptions
     * - defaultValue coercion
     */
    val actualSchema = GraphSdlSchema(File("src/test/sdl/schema.sdl")).toIntrospectionSchema().normalize()
    val expectedSchema = IntrospectionSchema(File("src/test/sdl/schema.json")).normalize()

    assertEquals(actualSchema.toString(), expectedSchema.toString())
  }

  private fun IntrospectionSchema.normalize(): IntrospectionSchema {
    return copy(types = toSortedMap().mapValues { (_, type) -> type.normalize() })
  }

  @Test
  fun `implementing an object fails`() {
    try {
      GraphSdlSchema(File("src/test/sdl/implements-object.sdl"))
      fail("parse expected to fail but was successful")
    } catch (e: ParseException) {
      assertThat(e.message).contains("Object `Cat` cannot implement non-interface `Animal`")
    }
  }

  @Test
  fun `writing SDL and parsing again yields identical schemas`() {
    val initialSchema = IntrospectionSchema(File("src/test/sdl/schema.json")).normalize()
    val sdlFile = File("build/sdl-test/schema.sdl")
    sdlFile.parentFile.deleteRecursively()
    sdlFile.parentFile.mkdirs()
    initialSchema.toSDL(sdlFile)
    val finalSchema = GraphSdlSchema(sdlFile).toIntrospectionSchema().normalize()

    dumpSchemas(initialSchema, finalSchema)
    assertEquals(initialSchema, finalSchema)
  }

  /**
   * use to make easier diffs
   */
  private fun dumpSchemas(expected: IntrospectionSchema, actual: IntrospectionSchema) {
    actual.wrap().toJson(File("build/sdl-test/actual.json"), "  ")
    expected.wrap().toJson(File("build/sdl-test/expected.json"), "  ")
  }
}
