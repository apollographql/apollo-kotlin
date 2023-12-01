package test

import com.apollographql.apollo3.api.getOrNull
import com.apollographql.apollo3.api.getOrThrow
import com.apollographql.apollo3.exception.ApolloGraphQLException
import `null`.PriceNullQuery
import `null`.ProductIgnoreErrorsQuery
import `null`.ProductNullQuery
import `null`.ProductQuery
import `null`.ProductResultQuery
import `null`.ProductThrowQuery
import `null`.UserNullQuery
import `null`.UserQuery
import `null`.UserResultQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CatchNullTest {
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

    assertNull(response.data?.user?.getOrNull()?.name)
  }

  @Test
  fun userNullOnUserNameError() {
    val response = UserNullQuery().parseResponse(userNameError)

    assertNull(response.data!!.user?.name)
  }

  @Test
  fun userOnUserSuccess() {
    val response = UserQuery().parseResponse(userSuccess)

    assertEquals("Pancakes", response.data!!.user?.name)
  }

  @Test
  fun userResultOnUserSuccess() {
    val response = UserResultQuery().parseResponse(userSuccess)

    assertEquals("Pancakes", response.data!!.user.getOrThrow().name)
  }

  @Test
  fun userNullOnUserSuccess() {
    val response = UserNullQuery().parseResponse(userSuccess)

    assertEquals("Pancakes", response.data!!.user!!.name)
  }

  @Test
  fun productOnProductPriceError() {
    val response = ProductQuery().parseResponse(productPriceError)

    assertNotNull(response.data)
    val exception = response.exception
    assertIs<ApolloGraphQLException>(exception)
    assertEquals("cannot resolve price", exception.error.message)
  }

  @Test
  fun productThrowOnProductPriceError() {
    val response = ProductThrowQuery().parseResponse(productPriceError)

    assertNull(response.data)
  }

  @Test
  fun productResultOnProductPriceError() {
    val response = ProductResultQuery().parseResponse(productPriceError)

    assertNull(null, response.data?.product?.getOrNull()?.price)
  }

  @Test
  fun productNullOnProductPriceError() {
    val response = ProductNullQuery().parseResponse(productPriceError)

    assertNull(response.data?.product?.price)
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
