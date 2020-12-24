package com.apollographql.apollo.response

import com.apollographql.apollo.api.CustomScalarTypeAdapter
import com.apollographql.apollo.api.JsonElement
import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ScalarTypeAdaptersTest {
  @Test
  fun customAdapterTakePrecedentOverDefault() {
    val customScalarTypeAdapters = mutableMapOf<ScalarType, CustomScalarTypeAdapter<*>>()
    val expectedAdapter = MockCustomScalarTypeAdapter()
    customScalarTypeAdapters[object : ScalarType {
      override val graphqlName = "String"
      override val className: String
        get() = String::class.java.name
    }] = expectedAdapter

    val actualAdapter = ScalarTypeAdapters(customScalarTypeAdapters).adapterFor<String>(object : ScalarType {
      override val graphqlName = "String"
      override val className: String
        get() = String::class.java.name
    })
    assertThat(actualAdapter).isEqualTo(expectedAdapter)
  }

  @Test(expected = IllegalArgumentException::class)
  fun missingAdapter() {
    ScalarTypeAdapters(emptyMap())
        .adapterFor<RuntimeException>(
            object : ScalarType {
              override val graphqlName = "RuntimeException"
              override val className: String
                get() = RuntimeException::class.java.name
            }
        )
  }

  @Test
  fun defaultStringAdapter() {
    val adapterScalar: CustomScalarTypeAdapter<String> = defaultAdapter(String::class.java)
    assertThat(adapterScalar.decode(JsonElement.fromRawValue("string"))).isEqualTo("string")
    assertThat(adapterScalar.encode("string").toRawValue()).isEqualTo("string")
  }

  @Test
  fun defaultBooleanAdapter() {
    val adapterScalar: CustomScalarTypeAdapter<Boolean> = defaultAdapter(Boolean::class.java)
    assertThat(adapterScalar.decode(JsonElement.fromRawValue(true))).isEqualTo(true)
    assertThat(adapterScalar.encode(true).toRawValue()).isEqualTo(true)
  }

  @Test
  fun defaultIntegerAdapter() {
    val adapterScalar: CustomScalarTypeAdapter<Int> = defaultAdapter(Int::class.java)
    assertThat(adapterScalar.decode(JsonElement.fromRawValue(100))).isEqualTo(100)
    assertThat(adapterScalar.encode(100).toRawValue()).isEqualTo(100)
  }

  @Test
  fun defaultLongAdapter() {
    val adapterScalar: CustomScalarTypeAdapter<Long> = defaultAdapter(Long::class.java)
    assertThat(adapterScalar.decode(JsonElement.fromRawValue(100L))).isEqualTo(100L)
    assertThat(adapterScalar.encode(100L).toRawValue()).isEqualTo(100L)
  }

  @Test
  fun defaultFloatAdapter() {
    val adapterScalar: CustomScalarTypeAdapter<Float> = defaultAdapter(Float::class.java)
    assertThat(adapterScalar.decode(JsonElement.fromRawValue(10.10f))).isWithin(0.0f).of(10.10f)
    assertThat(adapterScalar.encode(10.10f).toRawValue()).isEqualTo(10.10f)
  }

  @Test
  fun defaultDoubleAdapter() {
    val adapterScalar: CustomScalarTypeAdapter<Double> = defaultAdapter(Double::class.java)
    assertThat(adapterScalar.decode(JsonElement.fromRawValue(10.10))).isWithin(0.0).of(10.10)
    assertThat(adapterScalar.encode(10.10).toRawValue()).isEqualTo(10.10)
  }

  @Test
  fun defaultObjectAdapter() {
    val adapterScalar: CustomScalarTypeAdapter<Any> = defaultAdapter(Any::class.java)
    assertThat(adapterScalar.decode(JsonElement.fromRawValue(RuntimeException::class.java))).isEqualTo("class java.lang.RuntimeException")
    assertThat(adapterScalar.encode(RuntimeException::class.java).toRawValue()).isEqualTo("class java.lang.RuntimeException")
  }

  @Test
  fun defaultMapAdapter() {
    val value = mapOf<String, Any>(
        "key1" to "value1",
        "key2" to "value2"
    )
    val adapterScalar: CustomScalarTypeAdapter<Map<*, *>> = defaultAdapter(Map::class.java)
    assertThat(adapterScalar.decode(JsonElement.fromRawValue(value))).isEqualTo(value)
    assertThat(adapterScalar.encode(value).toRawValue()).isEqualTo(value)
  }

  @Test
  fun defaultListAdapter() {
    val value = listOf("item 1", "item 2")
    val adapterScalar: CustomScalarTypeAdapter<List<*>> = defaultAdapter(List::class.java)
    assertThat(adapterScalar.decode(JsonElement.fromRawValue(value))).isEqualTo(value)
    assertThat(adapterScalar.encode(value).toRawValue()).isEqualTo(value)
  }

  @Test
  fun defaultJsonString() {
    val actualObject = JsonElement.JsonObject(
        mapOf(
            "key" to JsonElement.JsonString("scalar"),
            "object" to JsonElement.JsonObject(mapOf("nestedKey" to JsonElement.JsonString("nestedScalar"))),
            "list" to JsonElement.JsonList(
                listOf(
                    JsonElement.JsonString("1"),
                    JsonElement.JsonString("2"),
                    JsonElement.JsonString("3"),
                )
            )
        )
    )
    val expectedJsonString = "{\"key\":\"scalar\",\"object\":{\"nestedKey\":\"nestedScalar\"},\"list\":[\"1\",\"2\",\"3\"]}"
    val adapter = defaultAdapter(String::class.java)
    assertThat(adapter.decode(actualObject)).isEqualTo(expectedJsonString)
  }

  private fun <T : Any> defaultAdapter(clazz: Class<T>): CustomScalarTypeAdapter<T> {
    return ScalarTypeAdapters(emptyMap()).adapterFor<T>(
        object : ScalarType {
          override val graphqlName: String
            get() = clazz.simpleName
          override val className: String
            get() = clazz.name
        }
    )
  }

  private inner class MockCustomScalarTypeAdapter : CustomScalarTypeAdapter<Any?> {
    override fun decode(jsonElement: JsonElement): Any {
      throw UnsupportedOperationException()
    }

    override fun encode(value: Any?): JsonElement {
      throw UnsupportedOperationException()
    }
  }
}