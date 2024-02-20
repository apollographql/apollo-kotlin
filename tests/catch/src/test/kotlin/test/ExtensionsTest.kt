package test

import com.apollographql.apollo3.api.graphQLErrorOrNull
import extensions.ExtensionsListQuery
import extensions.ExtensionsNullableQuery
import junit.framework.TestCase.assertTrue
import org.intellij.lang.annotations.Language
import kotlin.test.Test
import kotlin.test.assertNull

class ExtensionsTest {
  @Test
  fun scalarCatchToExtension() {
    @Language("json")
    val jsonResponse = """
    {
      "errors": [
         { "path": ["nullable"], "message": "resolve error" }   
      ], 
      "data": {
        "nullable": null
      }
    }
  """.trimIndent()

    ExtensionsNullableQuery().parseResponse(jsonResponse).apply {
      // List items are non-null
      assertTrue(data!!.nullable.graphQLErrorOrNull()?.message?.contains("resolve error") == true)
      assertNull(exception)
    }
  }

  @Test
  fun listCatchToExtension() {
    @Language("json")
    val jsonResponse = """
    {
      "errors": [
         { "path": ["list", 0], "message": "resolve error" }   
      ], 
      "data": {
        "list": [null]
      }
    }
  """.trimIndent()

    ExtensionsListQuery().parseResponse(jsonResponse).apply {
      // List items are non-null
      assertTrue(data!!.list?.get(0)?.graphQLErrorOrNull()?.message?.contains("resolve error") == true)
      assertNull(exception)
    }
  }
}