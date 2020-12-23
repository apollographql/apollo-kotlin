package com.apollographql.apollo.api.internal.json

import com.apollographql.apollo.api.BigDecimal
import com.apollographql.apollo.api.CustomScalarTypeAdapter
import com.apollographql.apollo.api.JsonElement
import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.internal.InputFieldMarshaller
import com.apollographql.apollo.api.internal.InputFieldWriter
import com.apollographql.apollo.api.toNumber
import okio.Buffer
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

class InputFieldJsonWriterTest {
  private val jsonBuffer = Buffer()
  private val jsonWriter = JsonWriter.of(jsonBuffer).apply {
    serializeNulls = true
    beginObject()
  }
  private val inputFieldJsonWriter = InputFieldJsonWriter(jsonWriter, ScalarTypeAdapters(emptyMap()))

  @Test
  fun writeString() {
    inputFieldJsonWriter.writeString("someField", "someValue")
    inputFieldJsonWriter.writeString("someNullField", null)
    assertEquals("{\"someField\":\"someValue\",\"someNullField\":null", jsonBuffer.readUtf8())
  }

  @Test
  fun writeInt() {
    inputFieldJsonWriter.writeInt("someField", 1)
    inputFieldJsonWriter.writeInt("someNullField", null)
    assertEquals("{\"someField\":1,\"someNullField\":null", jsonBuffer.readUtf8())
  }

  @Test
  fun writeLong() {
    inputFieldJsonWriter.writeLong("someField", 10L)
    inputFieldJsonWriter.writeLong("someNullField", null)
    assertEquals("{\"someField\":10,\"someNullField\":null", jsonBuffer.readUtf8())
  }

  @Test
  fun writeDouble() {
    inputFieldJsonWriter.writeDouble("someField", 1.01)
    inputFieldJsonWriter.writeDouble("someNullField", null)
    assertEquals("{\"someField\":1.01,\"someNullField\":null", jsonBuffer.readUtf8())
  }

  @Test
  fun writeNumber() {
    inputFieldJsonWriter.writeNumber("someField", BigDecimal("1.001").toNumber())
    inputFieldJsonWriter.writeNumber("someNullField", null)
    kotlin.test.assertEquals("{\"someField\":1.001,\"someNullField\":null", jsonBuffer.readUtf8())
  }

  @Test
  fun writeBoolean() {
    inputFieldJsonWriter.writeBoolean("someField", true)
    inputFieldJsonWriter.writeBoolean("someNullField", null)
    assertEquals("{\"someField\":true,\"someNullField\":null", jsonBuffer.readUtf8())
  }

  @Test
  fun writeObject() {
    inputFieldJsonWriter.writeObject("someField", object : InputFieldMarshaller {
      override fun marshal(writer: InputFieldWriter) {
        writer.writeString("someField", "someValue")
      }
    })
    inputFieldJsonWriter.writeObject("someNullField", null)
    assertEquals("{\"someField\":{\"someField\":\"someValue\"},\"someNullField\":null", jsonBuffer.readUtf8())
  }

  @Test
  fun writeList() {
    inputFieldJsonWriter.writeList("someField", object : InputFieldWriter.ListWriter {
      override fun write(listItemWriter: InputFieldWriter.ListItemWriter) {
        listItemWriter.writeString("someValue")
      }
    })
    inputFieldJsonWriter.writeList("someNullField", null)
    assertEquals("{\"someField\":[\"someValue\"],\"someNullField\":null", jsonBuffer.readUtf8())
  }

  @Test
  fun writeCustomBoolean() {
    val customScalarTypeAdapters: MutableMap<ScalarType, CustomScalarTypeAdapter<*>> = HashMap()
    val scalarType = MockCustomScalarType(JsonElement.JsonBoolean::class, "com.apollographql.apollo.api.JsonElement.GraphQLBoolean")
    customScalarTypeAdapters[scalarType] = object : MockCustomScalarTypeAdapter() {
      override fun encode(value: Any?): JsonElement {
        return JsonElement.JsonBoolean((value as Boolean))
      }
    }
    val inputFieldJsonWriter = InputFieldJsonWriter(jsonWriter, ScalarTypeAdapters(customScalarTypeAdapters))
    inputFieldJsonWriter.writeCustom("someField", scalarType, true)
    inputFieldJsonWriter.writeCustom("someNullField", scalarType, null)
    assertEquals("{\"someField\":true,\"someNullField\":null", jsonBuffer.readUtf8())
  }

  @Test
  fun writeCustomNumber() {
    val customScalarTypeAdapters: MutableMap<ScalarType, CustomScalarTypeAdapter<*>> = HashMap()
    val scalarType = MockCustomScalarType(JsonElement.JsonNumber::class, "com.apollographql.apollo.api.JsonElement.GraphQLNumber")
    customScalarTypeAdapters[scalarType] = object : MockCustomScalarTypeAdapter() {
      override fun encode(value: Any?): JsonElement {
        return JsonElement.JsonNumber((value as BigDecimal).toNumber())
      }
    }
    val inputFieldJsonWriter = InputFieldJsonWriter(jsonWriter, ScalarTypeAdapters(customScalarTypeAdapters))
    inputFieldJsonWriter.writeCustom("someField", scalarType, BigDecimal("100.1"))
    inputFieldJsonWriter.writeCustom("someNullField", scalarType, null)
    assertEquals("{\"someField\":100.1,\"someNullField\":null", jsonBuffer.readUtf8())
  }

  @Test
  fun writeCustomString() {
    val customScalarTypeAdapters: MutableMap<ScalarType, CustomScalarTypeAdapter<*>> = HashMap()
    val scalarType = MockCustomScalarType(JsonElement.JsonString::class, "com.apollographql.apollo.api.JsonElement.JsonString")
    customScalarTypeAdapters[scalarType] = object : MockCustomScalarTypeAdapter() {
      override fun encode(value: Any?): JsonElement {
        return JsonElement.JsonString((value as String))
      }
    }
    val inputFieldJsonWriter = InputFieldJsonWriter(jsonWriter, ScalarTypeAdapters(customScalarTypeAdapters))
    inputFieldJsonWriter.writeCustom("someField", scalarType, "someValue")
    inputFieldJsonWriter.writeCustom("someNullField", scalarType, null)
    assertEquals("{\"someField\":\"someValue\",\"someNullField\":null", jsonBuffer.readUtf8())
  }

  @Test
  fun writeCustomNull() {
    val customScalarTypeAdapters: MutableMap<ScalarType, CustomScalarTypeAdapter<*>> = HashMap()
    val scalarType = MockCustomScalarType(JsonElement.JsonNumber::class, "com.apollographql.apollo.api.JsonElement.JsonNumber")
    customScalarTypeAdapters[scalarType] = object : MockCustomScalarTypeAdapter() {
      override fun encode(value: Any?): JsonElement {
        return JsonElement.JsonNull
      }
    }
    val inputFieldJsonWriter = InputFieldJsonWriter(jsonWriter, ScalarTypeAdapters(customScalarTypeAdapters))
    inputFieldJsonWriter.writeCustom("someField", scalarType, null)
    assertEquals("{\"someField\":null", jsonBuffer.readUtf8())
  }

  @Test
  fun writeCustomJsonObject() {
    val value = mapOf(
        "stringField" to "string",
        "booleanField" to true,
        "numberField" to 100,
        "listField" to listOf(
            "string",
            true,
            100,
            mapOf(
                "stringField" to "string",
                "numberField" to 100,
                "booleanField" to true,
                "listField" to listOf(1, 2, 3)
            )
        ),
        "objectField" to mapOf(
            "stringField" to "string",
            "numberField" to 100,
            "booleanField" to true,
            "listField" to listOf(1, 2, 3)
        )
    )
    val scalarType = MockCustomScalarType(Map::class, "kotlin.collections.Map")
    inputFieldJsonWriter.writeCustom("someField", scalarType, value)
    inputFieldJsonWriter.writeCustom("someNullField", scalarType, null)
    assertEquals("{\"someField\":{\"stringField\":\"string\",\"booleanField\":true,\"numberField\":100,\"listField\":[\"string\",true,100,{\"stringField\":\"string\",\"numberField\":100,\"booleanField\":true,\"listField\":[1,2,3]}],\"objectField\":{\"stringField\":\"string\",\"numberField\":100,\"booleanField\":true,\"listField\":[1,2,3]}},\"someNullField\":null", jsonBuffer.readUtf8())
  }

  @Test
  fun writeCustomList() {
    val value = listOf(
        "string",
        true,
        100,
        mapOf(
            "stringField" to "string",
            "numberField" to 100,
            "booleanField" to true,
            "listField" to listOf(1, 2, 3)
        )
    )
    val scalarType = MockCustomScalarType(List::class, "kotlin.collections.List")
    inputFieldJsonWriter.writeCustom("someField", scalarType, value)
    inputFieldJsonWriter.writeCustom("someNullField", scalarType, null)
    assertEquals("{\"someField\":[\"string\",true,100,{\"stringField\":\"string\",\"numberField\":100,\"booleanField\":true,\"listField\":[1,2,3]}],\"someNullField\":null", jsonBuffer.readUtf8())
  }

  @Test
  fun writeListOfList() {
    inputFieldJsonWriter.writeList("someField", object : InputFieldWriter.ListWriter {
      override fun write(listItemWriter: InputFieldWriter.ListItemWriter) {
        listItemWriter.writeList(object : InputFieldWriter.ListWriter {
          override fun write(listItemWriter: InputFieldWriter.ListItemWriter) {
            listItemWriter.writeString("someValue")
          }
        })
      }
    })
    inputFieldJsonWriter.writeList("someNullField", null)
    assertEquals("{\"someField\":[[\"someValue\"]],\"someNullField\":null", jsonBuffer.readUtf8())
  }

  private data class MockCustomScalarType(val clazz: KClass<*>, override val className: String) : ScalarType {
    override val graphqlName
      get() = clazz.simpleName!!
  }

  private abstract inner class MockCustomScalarTypeAdapter : CustomScalarTypeAdapter<Any?> {
    override fun decode(jsonElement: JsonElement): Any {
      throw UnsupportedOperationException()
    }
  }
}
