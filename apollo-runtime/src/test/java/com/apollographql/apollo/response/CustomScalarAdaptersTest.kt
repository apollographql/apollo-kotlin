package com.apollographql.apollo.response

import com.apollographql.apollo.api.CustomScalarAdapter
import com.apollographql.apollo.api.JsonElement
import com.apollographql.apollo.api.CustomScalar
import com.apollographql.apollo.api.CustomScalarAdapters
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CustomScalarAdaptersTest {
  @Test
  fun customAdapterTakePrecedentOverDefault() {
    val customScalarAdapters = mutableMapOf<CustomScalar, CustomScalarAdapter<*>>()
    val expectedAdapter = MockCustomScalarAdapter()
    customScalarAdapters[object : CustomScalar {
      override val graphqlName = "String"
      override val className: String
        get() = String::class.java.name
    }] = expectedAdapter

    val actualAdapter = CustomScalarAdapters(customScalarAdapters).adapterFor<String>(object : CustomScalar {
      override val graphqlName = "String"
      override val className: String
        get() = String::class.java.name
    })
    assertThat(actualAdapter).isEqualTo(expectedAdapter)
  }

  @Test(expected = IllegalArgumentException::class)
  fun missingAdapter() {
    CustomScalarAdapters(emptyMap())
        .adapterFor<RuntimeException>(
            object : CustomScalar {
              override val graphqlName = "RuntimeException"
              override val className: String
                get() = RuntimeException::class.java.name
            }
        )
  }

  @Test
  fun defaultStringAdapter() {
    val adapterScalar: CustomScalarAdapter<String> = defaultAdapter(String::class.java)
    assertThat(adapterScalar.decode(JsonElement.fromRawValue("string"))).isEqualTo("string")
    assertThat(adapterScalar.encode("string").toRawValue()).isEqualTo("string")
  }

  @Test
  fun defaultBooleanAdapter() {
    val adapterScalar: CustomScalarAdapter<Boolean> = defaultAdapter(Boolean::class.java)
    assertThat(adapterScalar.decode(JsonElement.fromRawValue(true))).isEqualTo(true)
    assertThat(adapterScalar.encode(true).toRawValue()).isEqualTo(true)
  }

  @Test
  fun defaultIntegerAdapter() {
    val adapterScalar: CustomScalarAdapter<Int> = defaultAdapter(Int::class.java)
    assertThat(adapterScalar.decode(JsonElement.fromRawValue(100))).isEqualTo(100)
    assertThat(adapterScalar.encode(100).toRawValue()).isEqualTo(100)
  }

  @Test
  fun defaultLongAdapter() {
    val adapterScalar: CustomScalarAdapter<Long> = defaultAdapter(Long::class.java)
    assertThat(adapterScalar.decode(JsonElement.fromRawValue(100L))).isEqualTo(100L)
    assertThat(adapterScalar.encode(100L).toRawValue()).isEqualTo(100L)
  }

  @Test
  fun defaultFloatAdapter() {
    val adapterScalar: CustomScalarAdapter<Float> = defaultAdapter(Float::class.java)
    assertThat(adapterScalar.decode(JsonElement.fromRawValue(10.10f))).isWithin(0.0f).of(10.10f)
    assertThat(adapterScalar.encode(10.10f).toRawValue()).isEqualTo(10.10f)
  }

  @Test
  fun defaultDoubleAdapter() {
    val adapterScalar: CustomScalarAdapter<Double> = defaultAdapter(Double::class.java)
    assertThat(adapterScalar.decode(JsonElement.fromRawValue(10.10))).isWithin(0.0).of(10.10)
    assertThat(adapterScalar.encode(10.10).toRawValue()).isEqualTo(10.10)
  }

  @Test
  fun defaultObjectAdapter() {
    val adapterScalar: CustomScalarAdapter<Any> = defaultAdapter(Any::class.java)
    assertThat(adapterScalar.decode(JsonElement.fromRawValue(RuntimeException::class.java))).isEqualTo("class java.lang.RuntimeException")
    assertThat(adapterScalar.encode(RuntimeException::class.java).toRawValue()).isEqualTo("class java.lang.RuntimeException")
  }

  @Test
  fun defaultMapAdapter() {
    val value = mapOf<String, Any>(
        "key1" to "value1",
        "key2" to "value2"
    )
    val adapterScalar: CustomScalarAdapter<Map<*, *>> = defaultAdapter(Map::class.java)
    assertThat(adapterScalar.decode(JsonElement.fromRawValue(value))).isEqualTo(value)
    assertThat(adapterScalar.encode(value).toRawValue()).isEqualTo(value)
  }

  @Test
  fun defaultListAdapter() {
    val value = listOf("item 1", "item 2")
    val adapterScalar: CustomScalarAdapter<List<*>> = defaultAdapter(List::class.java)
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

  private fun <T : Any> defaultAdapter(clazz: Class<T>): CustomScalarAdapter<T> {
    return CustomScalarAdapters(emptyMap()).adapterFor<T>(
        object : CustomScalar {
          override val graphqlName: String
            get() = clazz.simpleName
          override val className: String
            get() = clazz.name
        }
    )
  }

  private inner class MockCustomScalarAdapter : CustomScalarAdapter<Any?> {
    override fun decode(jsonElement: JsonElement): Any {
      throw UnsupportedOperationException()
    }

    override fun encode(value: Any?): JsonElement {
      throw UnsupportedOperationException()
    }
  }
}