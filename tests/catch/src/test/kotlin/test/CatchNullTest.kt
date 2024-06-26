package test

import com.apollographql.apollo3.api.getOrNull
import com.apollographql.apollo3.api.getOrThrow
import `null`.NullAndNonNullQuery
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CatchNullTest {
  @Test
  fun userOnUserNameError() {
    val response = UserQuery().parseResponse(userNameError)
    assertNull(response.exception)
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
    assertNull(response.exception)
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

  @Test
  fun nullAndNonNull() {
    val response = NullAndNonNullQuery().parseResponse("""
      {
        "data": { "nonNull": 42, "nullable": null }
      }
    """.trimIndent())

    // plus(0) is only used to check that `nonNull` is non nullable
    assertEquals(42, response.data!!.nonNull?.plus(0))
  }
}
