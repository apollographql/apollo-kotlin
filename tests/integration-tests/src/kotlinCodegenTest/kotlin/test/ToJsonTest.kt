package test

import com.apollographql.apollo3.api.toJson
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.testing.internal.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ToJsonTest {
  @Test
  fun toJson() = runTest {
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("Luke"))

    assertEquals("{\"hero\":{\"name\":\"Luke\"}}", data.toJson())
  }
}
