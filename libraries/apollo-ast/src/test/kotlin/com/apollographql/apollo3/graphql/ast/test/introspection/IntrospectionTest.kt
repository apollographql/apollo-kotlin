package com.apollographql.apollo3.graphql.ast.test.introspection

import com.apollographql.apollo3.ast.SourceAwareException
import com.apollographql.apollo3.ast.introspection.IntrospectionSchema
import com.apollographql.apollo3.ast.introspection.toIntrospectionSchema
import com.apollographql.apollo3.ast.introspection.toSchemaGQLDocument
import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.ast.validateAsSchema
import okio.Buffer
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class IntrospectionTest {
  @Test
  fun parseSchema() {
    try {
      File("src/test/kotlin/com/apollographql/apollo3/graphql/ast/test/introspection/duplicate.json").toSchemaGQLDocument().validateAsSchema().getOrThrow()
    } catch (e: SourceAwareException) {
      assert(e.message!!.contains("is defined multiple times"))
    }
  }

  /**
   * Default values are encoded as Json strings representing their GraphQL value
   */
  @Test
  fun defaultValues() {
    val schema = """
      type Query {
         someField(arg: SomeInput): Int
      }
      
      input SomeInput {
         value1: Boolean = false
         value2: Direction = South 
         value3: OtherInput = {value: true, value2: [North, null, South]}
         value4: [Int] = [0, null, 1]
         value5: String = "South" 
      }
      
      input OtherInput {
         value1: Boolean = false
         value2: [Direction] = South 
      }
      
      enum Direction {
        South,
        North
      }
    """.trimIndent()

    val introspectionSchema = Buffer().writeUtf8(schema)
        .toSchema()
        .toIntrospectionSchema()

    val someInputType = introspectionSchema.__schema.types.first { it.name == "SomeInput" } as IntrospectionSchema.Schema.Type.InputObject
    val someInputFields = someInputType.inputFields

    assertEquals("false", someInputFields.first { it.name == "value1" }.defaultValue)
    assertEquals("South", someInputFields.first { it.name == "value2" }.defaultValue)
    assertEquals("{\nvalue: true\nvalue2: [North,null,South]\n}\n", someInputFields.first { it.name == "value3" }.defaultValue)
    assertEquals("[0,null,1]", someInputFields.first { it.name == "value4" }.defaultValue)
    assertEquals("\"South\"", someInputFields.first { it.name == "value5" }.defaultValue)
  }
}
