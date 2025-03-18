package test

import com.apollographql.apollo.exception.ApolloGraphQLException
import custom.GetFooQuery
import custom.GetNodeQuery
import custom.GetProductQuery
import org.intellij.lang.annotations.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class CustomTest {
  @Test
  fun errorPropagatesToData() {
    @Language("json")
    val jsonResponse = """
    {
      "errors": [
        { "path": ["foo"], "message": "foo error" }      
      ],
      "data": {
        "foo": null
      }
    }
  """.trimIndent()

    GetFooQuery().parseResponse(jsonResponse).exception.apply {
      assertIs<ApolloGraphQLException>(this)
      assertEquals("foo error", this.error.message)
    }
  }

  @Test
  fun catchToNullIsRequired() {
    @Language("json")
    val jsonResponse = """
    {
      "errors": [
        { "path": ["node", "bar"], "message": "bar error" }      
      ],
      "data": {
        "node": { "bar":  null }
      }
    }
  """.trimIndent()

    GetNodeQuery().parseResponse(jsonResponse).apply {
      exception.apply {
        assertIs<ApolloGraphQLException>(this)
        assertEquals("bar error", this.error.message)
      }
    }
  }
}