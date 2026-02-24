package test

import com.apollographql.apollo.api.CustomScalarAdapters
import com.example.MyLongAdapter
import data.builders.GetIntQuery
import data.builders.builder.Data
import data.builders.builder.resolver.DefaultFakeResolver
import data.builders.type.Long2
import kotlin.test.Test
import kotlin.test.assertEquals

class TestFixturesTest {
  private val customScalarAdapters = CustomScalarAdapters.Builder()
      .add(Long2.type.name, MyLongAdapter)
      .build()
  
  @Test
  fun canReferenceTestFixturesSymbols() {
    val data = GetIntQuery.Data(DefaultFakeResolver(), customScalarAdapters) {
      nullableInt = null
      nonNullableInt = 42
    }

    assertEquals(null, data.nullableInt)
    assertEquals(42, data.nonNullableInt)
  }
}
