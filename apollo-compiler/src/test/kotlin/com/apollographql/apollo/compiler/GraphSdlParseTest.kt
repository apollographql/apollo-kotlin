package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.parser.Schema
import com.apollographql.apollo.compiler.parser.sdl.GraphSdlSchema
import com.apollographql.apollo.compiler.parser.sdl.toIntrospectionSchema
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class GraphSdlParseTest() {

  @Test
  fun `SDL schema parsed successfully and produced the same introspection schema`() {
    val actualSchema = GraphSdlSchema(File("src/test/sdl/schema.graphql")).toIntrospectionSchema().normalize()
    val expectedSchema = Schema(File("src/test/sdl/schema.json")).normalize()
    assertEquals(actualSchema.toString(), expectedSchema.toString())
  }

  private fun Schema.normalize(): Schema {
    return copy(types = toSortedMap().mapValues { (_, type) -> type.normalize() })
  }

  private fun Schema.Type.normalize(): Schema.Type {
    return when (this) {
      is Schema.Type.Scalar -> this
      is Schema.Type.Object -> copy(fields = fields?.sortedBy { field -> field.name })
      is Schema.Type.Interface -> copy(fields = fields?.sortedBy { field -> field.name })
      is Schema.Type.Union -> copy(fields = fields?.sortedBy { field -> field.name })
      is Schema.Type.Enum -> this
      is Schema.Type.InputObject -> copy(inputFields = inputFields.sortedBy { field -> field.name })
    }
  }
}
