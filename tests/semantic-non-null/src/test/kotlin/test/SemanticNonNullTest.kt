package test

import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.parseResponse
import com.example.Get1Query
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class SemanticNonNullTest {
  @Test
  fun semanticNonNull() {
    val response = Get1Query().parseResponse(Buffer().writeUtf8("{ \"data\": { \"semanticNonNull\": 42}}").jsonReader())
    assertEquals(42, response.data!!.semanticNonNull.plus(0))
  }
}
