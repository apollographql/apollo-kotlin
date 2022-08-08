package test

import data.builders.GetStuffQuery
import kotlin.test.Test
import kotlin.test.assertEquals

class DataBuilderTest {
  @Test
  fun simpleTest() {
    val data = GetStuffQuery.Data {
      nullableInt = null
      nonNullableInt = 42

      this["alias"] = 50
    }

    assertEquals(null, data.nullableInt)
    assertEquals(42, data.nonNullableInt)
    assertEquals(50, data.alias)
  }
}
