package test

import com.apollographql.apollo.api.toJson
import com.apollographql.apollo.integration.normalizer.HeroNameQuery
import com.apollographql.apollo.testing.internal.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ToJsonTest {
  @Test
  fun toJson() = runTest {
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("Luke"))

    assertEquals("{\"hero\":{\"name\":\"Luke\"}}", data.toJson())
  }
}
