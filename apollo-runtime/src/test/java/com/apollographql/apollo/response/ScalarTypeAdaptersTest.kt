package com.apollographql.apollo.response

import com.apollographql.apollo.api.CustomTypeAdapter
import com.apollographql.apollo.api.CustomTypeValue
import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ScalarTypeAdaptersTest {
  @Test
  fun customAdapterTakePrecedentOverDefault() {
    val customTypeAdapters = mutableMapOf<ScalarType, CustomTypeAdapter<*>>()
    val expectedAdapter = MockCustomTypeAdapter()
    customTypeAdapters[object : ScalarType {
      override fun typeName() = "String"
      override fun className() = String::class.java.name
    }] = expectedAdapter

    val actualAdapter = ScalarTypeAdapters(customTypeAdapters).adapterFor<String>(object : ScalarType {
      override fun typeName() = "String"
      override fun className() = String::class.java.name
    })
    assertThat(actualAdapter).isEqualTo(expectedAdapter)
  }

  @Test(expected = IllegalArgumentException::class)
  fun missingAdapter() {
    ScalarTypeAdapters(emptyMap<ScalarType, CustomTypeAdapter<*>>())
        .adapterFor<RuntimeException>(
            object : ScalarType {
              override fun typeName() = "RuntimeException"
              override fun className() = RuntimeException::class.java.name
            }
        )
  }

  @Test
  fun defaultStringAdapter() {
    val adapter: CustomTypeAdapter<String> = defaultAdapter(String::class.java)
    assertThat(adapter.decode(CustomTypeValue.fromRawValue("string"))).isEqualTo("string")
    assertThat(adapter.encode("string").value).isEqualTo("string")
  }

  @Test
  fun defaultBooleanAdapter() {
    val adapter: CustomTypeAdapter<Boolean> = defaultAdapter(Boolean::class.java)
    assertThat(adapter.decode(CustomTypeValue.fromRawValue(true))).isEqualTo(true)
    assertThat(adapter.encode(true).value).isEqualTo(true)
  }

  @Test
  fun defaultIntegerAdapter() {
    val adapter: CustomTypeAdapter<Int> = defaultAdapter(Int::class.java)
    assertThat(adapter.decode(CustomTypeValue.fromRawValue(100))).isEqualTo(100)
    assertThat(adapter.encode(100).value).isEqualTo(100)
  }

  @Test
  fun defaultLongAdapter() {
    val adapter: CustomTypeAdapter<Long> = defaultAdapter(Long::class.java)
    assertThat(adapter.decode(CustomTypeValue.fromRawValue(100L))).isEqualTo(100L)
    assertThat(adapter.encode(100L).value).isEqualTo(100L)
  }

  @Test
  fun defaultFloatAdapter() {
    val adapter: CustomTypeAdapter<Float> = defaultAdapter(Float::class.java)
    assertThat(adapter.decode(CustomTypeValue.fromRawValue(10.10f))).isWithin(0.0f).of(10.10f)
    assertThat(adapter.encode(10.10f).value).isEqualTo(10.10f)
  }

  @Test
  fun defaultDoubleAdapter() {
    val adapter: CustomTypeAdapter<Double> = defaultAdapter(Double::class.java)
    assertThat(adapter.decode(CustomTypeValue.fromRawValue(10.10))).isWithin(0.0).of(10.10)
    assertThat(adapter.encode(10.10).value).isEqualTo(10.10)
  }

  @Test
  fun defaultObjectAdapter() {
    val adapter: CustomTypeAdapter<Any> = defaultAdapter(Any::class.java)
    assertThat(adapter.decode(CustomTypeValue.fromRawValue(RuntimeException::class.java))).isEqualTo("class java.lang.RuntimeException")
    assertThat(adapter.encode(RuntimeException::class.java).value).isEqualTo("class java.lang.RuntimeException")
  }

  @Test
  fun defaultMapAdapter() {
    val value= mapOf<String, Any>(
        "key1" to "value1",
        "key2" to "value2"
    )
    val adapter: CustomTypeAdapter<Map<*, *>> = defaultAdapter(Map::class.java)
    assertThat(adapter.decode(CustomTypeValue.fromRawValue(value))).isEqualTo(value)
    assertThat(adapter.encode(value).value).isEqualTo(value)
  }

  @Test
  fun defaultListAdapter() {
    val value = listOf("item 1", "item 2")
    val adapter: CustomTypeAdapter<List<*>> = defaultAdapter(List::class.java)
    assertThat(adapter.decode(CustomTypeValue.fromRawValue(value))).isEqualTo(value)
    assertThat(adapter.encode(value).value).isEqualTo(value)
  }

  @Test
  fun defaultJsonString() {
    val actualObject = CustomTypeValue.GraphQLJsonObject(
        mapOf(
            "key" to "scalar",
            "object" to mapOf<String, Any>("nestedKey" to "nestedScalar"),
            "list" to listOf("1", "2", "3")
        )
    )
    val expectedJsonString = "{\"key\":\"scalar\",\"object\":{\"nestedKey\":\"nestedScalar\"},\"list\":[\"1\",\"2\",\"3\"]}"
    val adapter = defaultAdapter(String::class.java)
    assertThat(adapter.decode(actualObject)).isEqualTo(expectedJsonString)
  }

  private fun <T : Any> defaultAdapter(clazz: Class<T>): CustomTypeAdapter<T> {
    return ScalarTypeAdapters(emptyMap<ScalarType, CustomTypeAdapter<*>>()).adapterFor<T>(
        object : ScalarType {
          override fun typeName(): String = clazz.simpleName

          override fun className(): String = clazz.name
        }
    )
  }

  private inner class MockCustomTypeAdapter : CustomTypeAdapter<Any?> {
    override fun decode(value: CustomTypeValue<*>): Any {
      throw UnsupportedOperationException()
    }
    override fun encode(value: Any?): CustomTypeValue<*> {
      throw UnsupportedOperationException()
    }
  }
}