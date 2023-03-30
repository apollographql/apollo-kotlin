package com.apollographql.apollo3.graphql.ast.test

import com.apollographql.apollo3.ast.SDLWriter
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.internal.buffer
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.parseAsGQLType
import com.apollographql.apollo3.ast.removeLocation
import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.ast.toUtf8
import okio.Buffer
import org.junit.Ignore
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

  @Test
  fun typeExtensionsAreSerialized() {
    val schemaString = """
      schema {
        query: Query
      }
      
      type Query {
        field1: Int
      }
      
      interface Iface {
        field2: Int
      }
      
      enum Direction {
        SOUTH
      }
      
      input Params {
        field5: String
      }
      
      union Any = Query
      
      scalar Json
      
      directive @stuff on FIELD | FRAGMENT_SPREAD | INLINE_FRAGMENT | OBJECT 
      
      type Mutation {
        field4: Int
      }

      extend schema @stuff
       
      extend type Query {
        field3: Int
      }
      
      extend type Query implements Iface
            
      extend union Any = Query | Mutation
      
      extend schema {
        mutation: Mutation
      }
      
      extend input Params @stuff {
        field6: String
      }
      
      extend scalar Json @stuff
      
      
    """.trimIndent()

    val expected = Buffer().writeUtf8(schemaString).parseAsGQLDocument().getOrThrow()

    val serialized = expected.toUtf8()

    Buffer().writeUtf8(serialized).parseAsGQLDocument().getOrThrow()
    // Enable when we have hashCode and equals on GQLNode
    //assertEquals(expected.removeLocation(), actual.removeLocation())
  }
}
