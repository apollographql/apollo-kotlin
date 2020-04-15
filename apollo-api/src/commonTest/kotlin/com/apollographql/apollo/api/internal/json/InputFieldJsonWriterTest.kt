package com.apollographql.apollo.api.internal.json

import com.apollographql.apollo.api.BigDecimal
import com.apollographql.apollo.api.CustomTypeAdapter
import com.apollographql.apollo.api.CustomTypeValue
import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.internal.InputFieldMarshaller
import com.apollographql.apollo.api.internal.InputFieldWriter
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
    inputFieldJsonWriter.writeNumber("someField", BigDecimal("1.001"))
    inputFieldJsonWriter.writeNumber("someNullField", null)
    assertEquals("{\"someField\":1.001,\"someNullField\":null", jsonBuffer.readUtf8())
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
    val customTypeAdapters: MutableMap<ScalarType, CustomTypeAdapter<*>> = HashMap()
    customTypeAdapters[MockCustomScalarType(CustomTypeValue.GraphQLBoolean::class)] = object : MockCustomTypeAdapter() {
      override fun encode(value: Any?): CustomTypeValue<*> {
        return CustomTypeValue.GraphQLBoolean((value as Boolean))
      }
    }
    val inputFieldJsonWriter = InputFieldJsonWriter(jsonWriter, ScalarTypeAdapters(customTypeAdapters))
    inputFieldJsonWriter.writeCustom("someField", MockCustomScalarType(CustomTypeValue.GraphQLBoolean::class), true)
    inputFieldJsonWriter.writeCustom("someNullField", MockCustomScalarType(CustomTypeValue.GraphQLBoolean::class), null)
    assertEquals("{\"someField\":true,\"someNullField\":null", jsonBuffer.readUtf8())
  }

  @Test
  fun writeCustomNumber() {
    val customTypeAdapters: MutableMap<ScalarType, CustomTypeAdapter<*>> = HashMap()
    customTypeAdapters[MockCustomScalarType(CustomTypeValue.GraphQLNumber::class)] = object : MockCustomTypeAdapter() {
      override fun encode(value: Any?): CustomTypeValue<*> {
        return CustomTypeValue.GraphQLNumber((value as Number))
      }
    }
    val inputFieldJsonWriter = InputFieldJsonWriter(jsonWriter, ScalarTypeAdapters(customTypeAdapters))
    inputFieldJsonWriter.writeCustom("someField", MockCustomScalarType(CustomTypeValue.GraphQLNumber::class), BigDecimal("100.1"))
    inputFieldJsonWriter.writeCustom("someNullField", MockCustomScalarType(CustomTypeValue.GraphQLNumber::class), null)
    assertEquals("{\"someField\":100.1,\"someNullField\":null", jsonBuffer.readUtf8())
  }

  @Test
  fun writeCustomString() {
    val customTypeAdapters: MutableMap<ScalarType, CustomTypeAdapter<*>> = HashMap()
    customTypeAdapters[MockCustomScalarType(CustomTypeValue.GraphQLString::class)] = object : MockCustomTypeAdapter() {
      override fun encode(value: Any?): CustomTypeValue<*> {
        return CustomTypeValue.GraphQLString((value as String))
      }
    }
    val inputFieldJsonWriter = InputFieldJsonWriter(jsonWriter, ScalarTypeAdapters(customTypeAdapters))
    inputFieldJsonWriter.writeCustom("someField", MockCustomScalarType(CustomTypeValue.GraphQLString::class), "someValue")
    inputFieldJsonWriter.writeCustom("someNullField", MockCustomScalarType(CustomTypeValue.GraphQLString::class), null)
    assertEquals("{\"someField\":\"someValue\",\"someNullField\":null", jsonBuffer.readUtf8())
  }

  @Test
  fun writeCustomJsonObject() {
    val value = mapOf(
        "stringField" to "string",
        "booleanField" to true,
        "numberField" to BigDecimal(100),
        "listField" to listOf(
            "string",
            true,
            BigDecimal(100),
            mapOf(
                "stringField" to "string",
                "numberField" to BigDecimal(100),
                "booleanField" to true,
                "listField" to listOf(1, 2, 3)
            )
        ),
        "objectField" to mapOf(
            "stringField" to "string",
            "numberField" to BigDecimal(100),
            "booleanField" to true,
            "listField" to listOf(1, 2, 3)
        )
    )
    inputFieldJsonWriter.writeCustom("someField", MockCustomScalarType(Map::class), value)
    inputFieldJsonWriter.writeCustom("someNullField", MockCustomScalarType(Map::class), null)
    assertEquals("{\"someField\":{\"stringField\":\"string\",\"booleanField\":true,\"numberField\":100,\"listField\":[\"string\",true,100,{\"stringField\":\"string\",\"numberField\":100,\"booleanField\":true,\"listField\":[1,2,3]}],\"objectField\":{\"stringField\":\"string\",\"numberField\":100,\"booleanField\":true,\"listField\":[1,2,3]}},\"someNullField\":null", jsonBuffer.readUtf8())
  }

  @Test
  fun writeCustomList() {
    val value = listOf(
        "string",
        true,
        BigDecimal(100),
        mapOf(
            "stringField" to "string",
            "numberField" to BigDecimal(100),
            "booleanField" to true,
            "listField" to listOf(1, 2, 3)
        )
    )
    inputFieldJsonWriter.writeCustom("someField", MockCustomScalarType(List::class), value)
    inputFieldJsonWriter.writeCustom("someNullField", MockCustomScalarType(List::class), null)
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

  private data class MockCustomScalarType internal constructor(val clazz: KClass<*>) : ScalarType {
    override fun typeName(): String {
      return clazz.simpleName!!
    }

    override fun className(): String {
      return clazz.qualifiedName!!
    }
  }

  private abstract inner class MockCustomTypeAdapter : CustomTypeAdapter<Any?> {
    override fun decode(value: CustomTypeValue<*>): Any {
      throw UnsupportedOperationException()
    }
  }
}
