package test

import com.apollographql.apollo.api.ApolloResponse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal fun assertResponseListEquals(expectedResponseList: List<ApolloResponse<*>>, actualResponseList: List<ApolloResponse<*>>) {
  assertContentEquals(expectedResponseList, actualResponseList) { expectedResponse, actualResponse ->
    assertEquals(expectedResponse.data, actualResponse.data)
    assertContentEquals(expectedResponse.errors, actualResponse.errors) { expectedError, actualError ->
      assertEquals(expectedError.message, actualError.message)
      kotlin.test.assertContentEquals(expectedError.path, actualError.path)
    }
  }
}

internal fun <T> assertContentEquals(expected: List<T>?, actual: List<T>?, assertEquals: (T, T) -> Unit) {
  if (expected == null) {
    assertNull(actual)
    return
  }
  assertNotNull(actual)
  assertEquals(expected.size, actual.size)
  for (i in expected.indices) {
    assertEquals(expected[i], actual[i])
  }
}
