package test

import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.api.toJson
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import org.junit.Test
import kotlin.test.assertEquals

class ToJsonTest {
  /**
   * Tests that we can serialize a `Operation.Data` instance without an `Operation` instance
   */
  @Test
  fun toJsonTest() {
    val data = HeroNameQuery.Data(
        HeroNameQuery.Hero(
            "Luke"
        )
    )

    val output = buildJsonString {
      data.toJson(this)
    }
    assertEquals("{\"hero\":{\"name\":\"Luke\"}}", output)
  }
}
