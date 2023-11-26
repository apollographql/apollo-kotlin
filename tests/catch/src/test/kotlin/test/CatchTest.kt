package test

import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.errorOrNull
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.parseResponse
import com.apollographql.apollo3.api.valueOrThrow
import com.apollographql.apollo3.exception.ApolloGraphQLException
import com.example.UserNullQuery
import com.example.UserResultQuery
import com.example.UserThrowQuery
import okio.Buffer
import org.intellij.lang.annotations.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@Language("json")
val userNameError = """
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

class CatchTest {
  @Test
  fun userThrowOnUserNameError() {
    val response = UserThrowQuery().parseResponse(userNameError)
    val exception = response.exception
    assertIs<ApolloGraphQLException>(exception)
    assertEquals("cannot resolve name", exception.error.message)
  }

  @Test
  fun userResultOnUserNameError() {
    val response = UserResultQuery().parseResponse(userNameError)

    assertEquals("cannot resolve name", response.data?.user?.errorOrNull?.message)
  }
  @Test
  fun userNullOnUserNameError() {
    val response = UserNullQuery().parseResponse(userNameError)

    assertNull(response.data!!.user)
  }

  @Test
  fun userThrowOnUserSuccess() {
    val response = UserThrowQuery().parseResponse(userSuccess)

    assertEquals("Pancakes", response.data!!.user.name)
  }

  @Test
  fun userResultOnUserSuccess() {
    val response = UserResultQuery().parseResponse(userSuccess)

    assertEquals("Pancakes", response.data!!.user.valueOrThrow().name)
  }

  @Test
  fun userNullOnUserSuccess() {
    val response = UserNullQuery().parseResponse(userSuccess)

    assertEquals("Pancakes", response.data!!.user!!.name)
  }
}

private fun String.jsonReader(): JsonReader = Buffer().writeUtf8(this).jsonReader()

fun <D: Query.Data> Query<D>.parseResponse(json: String): ApolloResponse<D> = parseResponse(json.jsonReader(), null, CustomScalarAdapters.Empty, null)
