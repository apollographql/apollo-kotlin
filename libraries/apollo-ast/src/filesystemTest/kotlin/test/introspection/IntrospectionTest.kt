package test.introspection

import com.apollographql.apollo.ast.SourceAwareException
import com.apollographql.apollo.ast.introspection.toIntrospectionSchema
import com.apollographql.apollo.ast.toFullSchemaGQLDocument
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.toSchema
import com.apollographql.apollo.graphql.ast.test.CWD
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntrospectionTest {
  @Test
  fun parseSchema() {
    try {
      "${CWD}/src/filesystemTest/kotlin/test/introspection/duplicate.json"
          .toPath()
          .toGQLDocument(allowJson = true)
          .toSchema()
    } catch (e: SourceAwareException) {
      assertTrue(e.message!!.contains("is defined multiple times"))
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

    val introspectionSchema = schema
        .toGQLDocument()
        .toFullSchemaGQLDocument()
        .toIntrospectionSchema()
        .__schema

    val someInputType = introspectionSchema.types.first { it.name == "SomeInput" }
    val someInputFields = someInputType.inputFields

    assertEquals("false", someInputFields?.first { it.name == "value1" }?.defaultValue)
    assertEquals("South", someInputFields?.first { it.name == "value2" }?.defaultValue)
    assertEquals("{\nvalue: true\nvalue2: [North,null,South]\n}\n", someInputFields?.first { it.name == "value3" }?.defaultValue)
    assertEquals("[0,null,1]", someInputFields?.first { it.name == "value4" }?.defaultValue)
    assertEquals("\"South\"", someInputFields?.first { it.name == "value5" }?.defaultValue)
  }
}
