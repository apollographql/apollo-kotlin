package test

import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.parseData
import com.example.GetItemQuery
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class IncludeTest {
  @Test
  fun canParseInclude() {
    val jsonData = """
      {
        "item": {
          "__typename": "Item",
          "id": 42,
          "details": {
            "__typename": "Details",
            "id": 43,
            "title": "title",
            "summary": "summary",
            "tags": ["foo", "bar"]
          }
        }
      }
    """.trimIndent()

    listOf(true, false).forEach {
      val data = GetItemQuery(it).parseData(Buffer().writeUtf8(jsonData).jsonReader())
      assertEquals("title", data!!.item!!.details!!.title)
    }
  }
}
