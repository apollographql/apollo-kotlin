package test

import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.parseResponse
import com.apollographql.apollo3.exception.ApolloException
import com.example.GetNooeOrThrowQuery
import com.example.GetNooePartialQuery
import com.example.GetUserOrThrowQuery
import com.example.GetUserPartialQuery
import okio.Buffer
import org.intellij.lang.annotations.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class CatchTest {
  @Language("json")
  val userError = """
    {
      "errors": [
        {
          "path": ["user", "name"], 
          "message": "cannot resolve name"
        }
      ],
      "data": {
        "user": {
          "name": null
        }
      }
    }
  """.trimIndent()

  @Language("json")
  val userSuccess = """
    {
      "data": {
        "user": {
          "name": "Pancakes"
        }
      }
    }
  """.trimIndent()

  @Language("json")
  val nooeError = """
    {
      "errors": [
        {
          "path": ["nullOnlyOnError"], 
          "message": "cannot resolve nullOnlyOnError"
        }
      ],
      "data": {
        "nullOnlyOnError": null
      }
    }
  """.trimIndent()

  private fun String.jsonReader(): JsonReader = Buffer().writeUtf8(this).jsonReader()

  @Test
  fun simplePartial() {
    val response = GetNooePartialQuery().parseResponse(nooeError.jsonReader(), null, CustomScalarAdapters.Empty, null)

    assertEquals("cannot resolve nullOnlyOnError", response.data?.nullOnlyOnError?.errorsOrNull()?.single()?.message)
    assertNull(response.exception)
  }

  @Test
  fun simpleThrow() {
    val response = GetNooeOrThrowQuery().parseResponse(nooeError.jsonReader(), null, CustomScalarAdapters.Empty, null)

    assertNull(response.data)
    assertIs<ApolloException>(response.exception)
  }

  @Test
  fun userPartial() {
    val response = GetUserPartialQuery().parseResponse(userError.jsonReader(), null, CustomScalarAdapters.Empty, null)

    assertEquals("cannot resolve name", response.data?.user?.errorsOrNull()?.single()?.message)
    assertNull(response.exception)
  }

  @Test
  fun userThrow()  {
    val response = GetUserOrThrowQuery().parseResponse(userError.jsonReader(), null, CustomScalarAdapters.Empty, null)

    assertNull(response.data)
    assertIs<ApolloException>(response.exception)
  }

  @Test
  fun userSuccess()  {
    val response = GetUserOrThrowQuery().parseResponse(userSuccess.jsonReader(), null, CustomScalarAdapters.Empty, null)

    assertEquals("Pancakes", response.data?.user?.name)
    assertNull(response.exception)
  }
}
