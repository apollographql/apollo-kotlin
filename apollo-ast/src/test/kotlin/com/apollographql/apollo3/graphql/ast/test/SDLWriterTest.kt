package com.apollographql.apollo3.graphql.ast.test

import com.apollographql.apollo3.ast.SDLWriter
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.internal.buffer
import com.apollographql.apollo3.ast.toSchema
import okio.Buffer
import org.junit.Test
import kotlin.test.assertEquals

class SDLWriterTest {
  @Test
  fun schemaMayContainBuiltinDirectives() {
    val schemaString = """
      schema {
          query: Query
      }
      
      type Query {
          foo: Int
      }
      
      interface A {
          a: String
      }
      
      interface B {
          b: String
      }
      
      type C implements A & B {
          a: String
      
          b: String
      }
      
      interface D implements A & B {
          a: String
      
          b: String
      }

    """.trimIndent()

    val schema: Schema = schemaString.buffer().toSchema()
    val writerBuffer = Buffer()
    val sdlWriter = SDLWriter(writerBuffer, "    ")
    sdlWriter.write(schema.toGQLDocument())
    assertEquals(writerBuffer.readUtf8(), schemaString)
  }
}
