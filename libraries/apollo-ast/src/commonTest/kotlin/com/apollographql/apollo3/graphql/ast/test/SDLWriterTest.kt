package com.apollographql.apollo3.graphql.ast.test

import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.toSDL
import kotlin.test.Test
import kotlin.test.assertEquals

class SDLWriterTest {
  @Test
  fun simpleTest() {
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

    val schema: GQLDocument = schemaString.parseAsGQLDocument().getOrThrow()
    val sdl = schema.toSDL("    ")
    assertEquals(schemaString, sdl)
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

    val expected = schemaString.parseAsGQLDocument().getOrThrow()

    val serialized = expected.toSDL()

    serialized.parseAsGQLDocument().getOrThrow()
    // Enable when we have hashCode and equals on GQLNode
    //assertEquals(expected.removeLocation(), actual.removeLocation())
  }
}
