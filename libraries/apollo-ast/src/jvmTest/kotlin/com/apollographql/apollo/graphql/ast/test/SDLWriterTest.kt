package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.toSDL
import com.apollographql.apollo.ast.withBuiltinDefinitions
import com.apollographql.apollo.graphql.ast.test.ParserTest.Companion.checkExpected
import java.io.File
import kotlin.test.Test

class SDLWriterTest {
  @Test
  fun simpleTest() {
    val sdlSchema = File("${CWD}/test-fixtures/sdl/simple.graphqls")

    checkExpected(sdlSchema) {
      it.parseAsGQLDocument().getOrThrow().toSDL("    ")
    }
  }

  @Test
  fun typeRedefinitionInspectionIsIgnored() {
    val sdlSchema = File("${CWD}/test-fixtures/sdl/type_redefinitions.graphqls")

    checkExpected(sdlSchema) {
      it.parseAsGQLDocument().getOrThrow().withBuiltinDefinitions().toSDL("    ")
    }
  }

  @Test
  fun introspectionSchema() {
    val jsonSchema = File("${CWD}/test-fixtures/sdl/introspection.json")

    checkExpected(jsonSchema) {
      it.toGQLDocument(allowJson = true).toSDL("    ")
    }
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
