package test

import com.apollographql.apollo.api.http.internal.urlEncode
import kotlin.test.Test
import kotlin.test.assertEquals

class UrlEncodingTest {
  @Test
  fun test() {
    assertEquals("%20", " ".urlEncode())
    assertEquals("az-~._", "az-~._".urlEncode())
  }
}