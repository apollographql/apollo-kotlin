package test

import com.apollographql.apollo.api.graphQLErrorOrNull
import com.apollographql.apollo.api.getOrNull
import com.apollographql.apollo.api.getOrThrow
import result.PriceNullQuery
import result.ProductIgnoreErrorsQuery
import result.ProductNullQuery
import result.ProductQuery
import result.ProductResultQuery
import result.UserNullQuery
import result.UserQuery
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
}
