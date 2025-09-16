package test

import com.apollographql.apollo.api.getOrThrow
import com.apollographql.apollo.api.graphQLErrorOrNull
import com.apollographql.apollo.exception.ApolloGraphQLException
import `throw`.PriceNullQuery
import `throw`.ProductIgnoreErrorsQuery
import `throw`.ProductNullQuery
import `throw`.ProductQuery
import `throw`.ProductResultQuery
import `throw`.UserNullQuery
import `throw`.UserQuery
import `throw`.UserResultQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CatchThrowTest {
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

    assertNull(response.exception)
    assertEquals("cannot resolve name", response.data?.user?.graphQLErrorOrNull()?.message)
  }

  @Test
  fun userNullOnUserNameError() {
    val response = UserNullQuery().parseResponse(userNameError)

    assertNull(response.exception)
    assertNull(response.data!!.user)
  }

  @Test
  fun userOnUserSuccess() {
    val response = UserQuery().parseResponse(userSuccess)

    assertNull(response.exception)
    assertEquals("Pancakes", response.data!!.user.name)
  }

  @Test
  fun userResultOnUserSuccess() {
    val response = UserResultQuery().parseResponse(userSuccess)

    assertNull(response.exception)
    assertEquals("Pancakes", response.data!!.user.getOrThrow().name)
  }

  @Test
  fun userNullOnUserSuccess() {
    val response = UserNullQuery().parseResponse(userSuccess)

    assertNull(response.exception)
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

    assertNull(response.exception)
    assertEquals("cannot resolve price", response.data?.product?.graphQLErrorOrNull()?.message)
  }

  @Test
  fun productNullOnProductPriceError() {
    val response = ProductNullQuery().parseResponse(productPriceError)

    assertNull(response.exception)
    assertNull(response.data?.product)
    assertNotNull(response.data)
  }

  @Test
  fun productIgnoreErrorsOnProductPriceError() {
    val response = ProductIgnoreErrorsQuery().parseResponse(productPriceError)

    assertNull(response.exception)
    assertNotNull(response.data?.product)
    assertNull(response.data?.product?.price)
    assertEquals("cannot resolve price", response.errors?.single()?.message)
  }

  @Test
  fun productPriceNullOnProductPriceError() {
    val response = PriceNullQuery().parseResponse(productPriceError)

    assertNull(response.exception)
    assertNotNull(response.data?.product)
    assertNull(response.data?.product?.price)
    assertEquals("cannot resolve price", response.errors?.single()?.message)
  }

  @Test
  fun productPriceNullOnProductPriceNull() {
    val response = PriceNullQuery().parseResponse(productPriceNull)

    assertNull(response.exception)
    assertNotNull(response.data?.product)
    assertNull(response.data?.product?.price)
    assertNull(response.errors)
  }
}
