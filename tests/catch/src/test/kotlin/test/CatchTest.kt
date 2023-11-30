package test

import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.exceptionOrNull
import com.apollographql.apollo3.api.graphQLErrorOrNull
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.parseResponse
import com.apollographql.apollo3.api.valueOrThrow
import com.apollographql.apollo3.exception.ApolloGraphQLException
import default.PriceNullQuery
import default.ProductIgnoreErrorsQuery
import default.ProductNullQuery
import default.ProductQuery
import default.ProductResultQuery
import default.UserNullQuery
import default.UserQuery
import default.UserResultQuery
import okio.Buffer
import org.intellij.lang.annotations.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CatchTest {
  @Test
  fun userOnUserNameError() {
    val response = UserQuery().parseResponse(userNameError)
    val exception = response.exception
    assertIs<ApolloGraphQLException>(exception)
    assertEquals("cannot resolve name", exception.error.message)
  }

  @Test
  fun userResultOnUserNameError() {
    val response = UserResultQuery().parseResponse(userNameError)

    assertEquals("cannot resolve name", response.data?.user?.graphQLErrorOrNull()?.message)
  }

  @Test
  fun userNullOnUserNameError() {
    val response = UserNullQuery().parseResponse(userNameError)

    assertNull(response.data!!.user)
  }

  @Test
  fun userOnUserSuccess() {
    val response = UserQuery().parseResponse(userSuccess)

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

  @Test
  fun productOnProductPriceError() {
    val response = ProductQuery().parseResponse(productPriceError)

    val exception = response.exception
    assertIs<ApolloGraphQLException>(exception)
    assertEquals("cannot resolve price", exception.error.message)
  }

  @Test
  fun productResultOnProductPriceError() {
    val response = ProductResultQuery().parseResponse(productPriceError)

    assertEquals("cannot resolve price", response.data?.product?.graphQLErrorOrNull()?.message)
  }

  @Test
  fun productNullOnProductPriceError() {
    val response = ProductNullQuery().parseResponse(productPriceError)

    assertNull(response.data?.product)
    assertNotNull(response.data)
  }

  @Test
  fun productIgnoreErrorsOnProductPriceError() {
    val response = ProductIgnoreErrorsQuery().parseResponse(productPriceError)

    assertNotNull(response.data?.product)
    assertNull(response.data?.product?.price)
    assertEquals("cannot resolve price", response.errors?.single()?.message)
  }

  @Test
  fun productPriceNullOnProductPriceError() {
    val response = PriceNullQuery().parseResponse(productPriceError)

    assertNotNull(response.data?.product)
    assertNull(response.data?.product?.price)
    assertEquals("cannot resolve price", response.errors?.single()?.message)
  }

  @Test
  fun productPriceNullOnProductPriceNull() {
    val response = PriceNullQuery().parseResponse(productPriceNull)

    assertNotNull(response.data?.product)
    assertNull(response.data?.product?.price)
    assertNull(response.errors)
  }
}

private fun String.jsonReader(): JsonReader = Buffer().writeUtf8(this).jsonReader()

fun <D : Query.Data> Query<D>.parseResponse(json: String): ApolloResponse<D> = parseResponse(json.jsonReader(), null, CustomScalarAdapters.Empty, null)

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
val productPriceError = """
    {
      "errors": [
        {
          "path": ["product", "price"], 
          "message": "cannot resolve price"
        }
      ],
      "data": {
        "product": {
          "price": null
        }
      }
    }
  """.trimIndent()

@Language("json")
val productPriceNull = """
    {
      "data": {
        "product": {
          "price": null
        }
      }
    }
  """.trimIndent()