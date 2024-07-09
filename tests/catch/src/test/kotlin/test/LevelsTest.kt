package test

import com.apollographql.apollo.api.getOrThrow
import com.apollographql.apollo.api.graphQLErrorOrNull
import junit.framework.TestCase.assertTrue
import org.intellij.lang.annotations.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LevelsTest {
  @Test
  fun byDefaultListItemsAreNonNull() {
    @Language("json")
    val jsonResponse = """
    {
      "data": {
        "list1": [0, 1]
      }
    }
  """.trimIndent()

    `throw`.List1Query().parseResponse(jsonResponse).apply {
      // List items are non-null
      assertTrue(data!!.list1!!.get(1).minus(1) == 0)
      assertNull(exception)
    }
  }

  @Test
  fun canExtendSchemaTypes() {
    @Language("json")
    val jsonResponse = """
    {
      "data": {
        "list": [0, 1]
      }
    }
  """.trimIndent()

    `throw`.ListQuery().parseResponse(jsonResponse).apply {
      // List items are non-null
      assertTrue(data!!.list!!.get(1).minus(1) == 0)
      assertNull(exception)
    }
  }

  @Test
  fun canCatchItem() {
    @Language("json")
    val jsonResponse = """
    {
      "errors": [
        { "path": ["list1", 1], "message": "item error" }      
      ],
      "data": {
        "list1": [0, null]
      }
    }
  """.trimIndent()

    `throw`.ListCatchAllQuery().parseResponse(jsonResponse).apply {
      assertEquals("item error", data!!.list1.getOrThrow()?.get(1)?.graphQLErrorOrNull()?.message)
      assertNull(exception)
    }
  }

  @Test
  fun canCatchList() {
    @Language("json")
    val jsonResponse = """
    {
      "errors": [
        { "path": ["list1"], "message": "list error" }      
      ],
      "data": {
        "list1": null
      }
    }
  """.trimIndent()

    `throw`.ListCatchAllQuery().parseResponse(jsonResponse).apply {
      assertEquals("list error", data!!.list1.graphQLErrorOrNull()?.message)
      assertNull(exception)
    }
  }
}