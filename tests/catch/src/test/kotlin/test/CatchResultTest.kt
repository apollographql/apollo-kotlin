package test

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.FieldResult
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.graphQLErrorOrNull
import com.apollographql.apollo.api.getOrNull
import com.apollographql.apollo.api.getOrThrow
import com.apollographql.apollo.api.json.MapJsonWriter
import com.apollographql.apollo.exception.ApolloGraphQLException
import result.PriceNullQuery
import result.ProductIgnoreErrorsQuery
import result.ProductNullQuery
import result.ProductQuery
import result.ProductResultQuery
import result.UserNullQuery
import result.UserQuery
import result.UserResultAndProductQuery
import result.UserResultQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CatchResultTest {
  @Test
  fun userOnUserNameError() {
    val response = UserQuery().parseResponse(userNameError)

    assertNull(response.exception)
  }

  @Test
  fun userResultOnUserNameError() {
    val response = UserResultQuery().parseResponse(userNameError)

    assertEquals("cannot resolve name", response.data?.user?.getOrNull()?.name?.graphQLErrorOrNull()?.message)
  }

  @Test
  fun userNullOnUserNameError() {
    val response = UserNullQuery().parseResponse(userNameError)

    assertEquals("cannot resolve name", response.data!!.user?.name?.graphQLErrorOrNull()?.message)
  }

  @Test
  fun userOnUserSuccess() {
    val response = UserQuery().parseResponse(userSuccess)

    assertEquals("Pancakes", response.data!!.user.getOrNull()?.name?.getOrNull())
  }

  @Test
  fun userResultOnUserSuccess() {
    val response = UserResultQuery().parseResponse(userSuccess)

    assertEquals("Pancakes", response.data!!.user.getOrThrow().name.getOrNull())
  }

  @Test
  fun userNullOnUserSuccess() {
    val response = UserNullQuery().parseResponse(userSuccess)

    assertEquals("Pancakes", response.data!!.user!!.name.getOrNull())
  }

  @Test
  fun productOnProductPriceError() {
    val response = ProductQuery().parseResponse(productPriceError)

    assertNull(response.exception)
  }

  @Test
  fun productResultOnProductPriceError() {
    val response = ProductResultQuery().parseResponse(productPriceError)

    assertEquals("cannot resolve price", response.data?.product?.getOrNull()?.price?.graphQLErrorOrNull()?.message)
  }

  @Test
  fun productNullOnProductPriceError() {
    val response = ProductNullQuery().parseResponse(productPriceError)

    assertEquals("cannot resolve price", response.data?.product?.price?.graphQLErrorOrNull()?.message)
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
    assertNull(response.data?.product?.getOrNull()?.price)
    assertEquals("cannot resolve price", response.errors?.single()?.message)
  }

  @Test
  fun productPriceNullOnProductPriceNull() {
    val response = PriceNullQuery().parseResponse(productPriceNull)

    assertNotNull(response.data?.product)
    assertNull(response.data?.product?.getOrNull()?.price)
    assertNull(response.errors)
  }

  @Test
  fun composeFailureFollowedBySibling() {
    val data = UserResultAndProductQuery.Data(
        user = userFailure,
        product = FieldResult.Success(UserResultAndProductQuery.Product(price = FieldResult.Success("10"))),
    )

    assertEquals(
        mapOf("user" to null, "product" to mapOf("price" to "10")),
        UserResultAndProductQuery().compose(data)
    )
  }

  @Test
  fun composeFailureAsLastField() {
    val data = UserResultQuery.Data(user = userFailure)

    assertEquals(mapOf("user" to null), UserResultQuery().compose(data))
  }

  @Test
  fun composeRoundTripsParsedFailure() {
    val data = UserResultQuery().parseResponse(userNameError).data!!

    assertEquals(mapOf("user" to mapOf("name" to null)), UserResultQuery().compose(data))
  }
}

private val userFailure = FieldResult.Failure(
    ApolloGraphQLException(Error.Builder("cannot resolve user").path(listOf("user")).build())
)

private fun <D : Query.Data> Query<D>.compose(data: D): Any? {
  val writer = MapJsonWriter()
  adapter().toJson(writer, CustomScalarAdapters.Empty, data)
  return writer.root()
}
