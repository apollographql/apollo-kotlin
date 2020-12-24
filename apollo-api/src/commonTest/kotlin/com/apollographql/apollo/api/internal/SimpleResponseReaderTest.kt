package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.BigDecimal
import com.apollographql.apollo.api.CustomScalarTypeAdapter
import com.apollographql.apollo.api.JsonElement
import com.apollographql.apollo.api.EMPTY_OPERATION
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.internal.json.JsonDataException
import com.apollographql.apollo.api.internal.json.JsonUtf8Writer
import com.apollographql.apollo.api.internal.json.Utils
import okio.Buffer
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class SimpleResponseReaderTest {
  private val noConditions: List<ResponseField.Condition> = emptyList()

  @Test
  fun readString() {
    val successField = ResponseField.forString("successFieldResponseName", "successFieldName", null, false, noConditions)

    val responseReader = responseReader(
        mapOf(
            "successFieldResponseName" to "response1"
        )
    )

    assertEquals(expected = "response1", actual = responseReader.readString(successField))
  }

  @Test
  fun readInt() {
    val successField = ResponseField.forInt("successFieldResponseName", "successFieldName", null, false, noConditions)
    val responseReader = responseReader(
        mapOf(
            "successFieldResponseName" to BigDecimal(1),
            "classCastExceptionField" to "anything"
        )
    )

    assertEquals(expected = 1, actual = responseReader.readInt(successField))
  }

  @Test
  fun readDouble() {
    val successField = ResponseField.forDouble("successFieldResponseName", "successFieldName", null, false, noConditions)
    val responseReader = responseReader(
        mapOf(
            "successFieldResponseName" to BigDecimal(1.1),
            "classCastExceptionField" to "anything"
        )
    )

    assertEquals(expected = 1.1, actual = responseReader.readDouble(successField)!!)
  }

  @Test
  fun readBoolean() {
    val successField = ResponseField.forBoolean("successFieldResponseName", "successFieldName", null, false, noConditions)
    val responseReader = responseReader(
        mapOf(
            "successFieldResponseName" to true,
            "classCastExceptionField" to "anything"
        )
    )

    assertTrue(responseReader.readBoolean(successField)!!)
  }

  @Test
  fun readObject() {
    val responseObject = Any()
    val successField = ResponseField.forObject("successFieldResponseName", "successFieldName", null, false, noConditions)
    val responseReader = responseReader(
        mapOf(
            "successFieldResponseName" to emptyMap<Any, Any>(),
            "classCastExceptionField" to "anything"
        )
    )

    assertEquals(
        expected = responseObject,
        actual = responseReader.readObject(successField) { responseObject }
    )
  }

  @Test
  fun readCustomObjectMap() {
    val mapScalarType: ScalarType = object : ScalarType {
      override val graphqlName = "CustomObject"
      override val className = "kotlin.collections.Map"
    }

    val successField = ResponseField.forCustomScalar(
        "successFieldResponseName", "successFieldName", null,
        false, mapScalarType, noConditions
    )

    val objectMap = mapOf(
        "string" to "string",
        "boolean" to true,
        "double" to 1.99,
        "float" to 2.99f,
        "long" to 3L,
        "int" to 4,
        "stringList" to listOf("string1", "string2"),
        "booleanList" to listOf("true", "false"),
        "doubleList" to listOf(1.99, 2.99),
        "floatList" to listOf(3.99f, 4.99f, 5.99f),
        "longList" to listOf(5L, 7L),
        "intList" to listOf(8, 9, 10),
        "object" to emptyMap<String, Any>(),
        "objectList" to listOf(emptyMap<String, Any>(), emptyMap())
    )

    val responseReader = responseReader(
        mapOf(
            "successFieldResponseName" to objectMap
        )
    )

    assertEquals(expected = objectMap.toString(), actual = responseReader.readCustomScalar<Map<*, *>>(successField).toString())
  }

  @Test
  fun readCustomObjectList() {
    val listScalarType: ScalarType = object : ScalarType {
      override val graphqlName = "CustomList"

      override val className = "kotlin.collections.List"
    }

    val successField = ResponseField.forCustomScalar(
        "successFieldResponseName", "successFieldName", null,
        false, listScalarType, noConditions
    )

    val objectMap = mapOf(
        "string" to "string",
        "boolean" to true,
        "object" to emptyMap<String, Any>(),
        "objectList" to listOf(emptyMap<String, Any>(), emptyMap())
    )

    val objectList = listOf(objectMap, objectMap)

    val responseReader = responseReader(
        mapOf(
            "successFieldResponseName" to objectList
        )
    )

    assertEquals(expected = objectList.toString(), actual = responseReader.readCustomScalar<List<*>>(successField).toString())
  }

  @Test
  fun readCustomWithDefaultAdapter() {
    val stringField = ResponseField.forCustomScalar(
        "stringField", "stringField", null, false,
        scalarTypeFor(String::class, "kotlin.String"), noConditions
    )
    val booleanField = ResponseField.forCustomScalar(
        "booleanField", "booleanField", null, false,
        scalarTypeFor(Boolean::class, "kotlin.Boolean"), noConditions
    )
    val integerField = ResponseField.forCustomScalar(
        "integerField", "integerField", null, false,
        scalarTypeFor(Int::class, "kotlin.Int"), noConditions
    )
    val longField = ResponseField.forCustomScalar(
        "longField", "longField", null, false,
        scalarTypeFor(Long::class, "kotlin.Long"), noConditions
    )
    val floatField = ResponseField.forCustomScalar(
        "floatField", "floatField", null, false,
        scalarTypeFor(Float::class, "kotlin.Float"), noConditions
    )
    val doubleField = ResponseField.forCustomScalar(
        "doubleField", "doubleField", null, false,
        scalarTypeFor(Double::class, "kotlin.Double"), noConditions
    )
    val unsupportedField = ResponseField.forCustomScalar(
        "unsupportedField", "unsupportedField", null, false,
        scalarTypeFor(RuntimeException::class, "kotlin.RuntimeException"), noConditions
    )

    val responseReader = responseReader(
        mapOf(
            stringField.responseName to "string",
            booleanField.responseName to true,
            integerField.responseName to BigDecimal(1),
            longField.responseName to BigDecimal(2),
            floatField.responseName to BigDecimal("3.99"),
            doubleField.responseName to BigDecimal("4.99"),
            unsupportedField.responseName to "smth",
        )
    )

    assertEquals(expected = "string", actual = responseReader.readCustomScalar(stringField)!!)
    assertEquals(expected = true, actual = responseReader.readCustomScalar(booleanField)!!)
    assertEquals(expected = 1, actual = responseReader.readCustomScalar(integerField)!!)
    assertEquals(expected = 2L, actual = responseReader.readCustomScalar(longField)!!)
    assertEquals(expected = 3.99f, actual = responseReader.readCustomScalar(floatField)!!)
    assertEquals(expected = 4.99, actual = responseReader.readCustomScalar(doubleField)!!)

    try {
      responseReader.readCustomScalar<Any>(unsupportedField)
      fail("Expect IllegalArgumentException")
    } catch (expected: IllegalArgumentException) {
      // expected
    }
  }

  @Test
  fun readStringList() {
    val successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions)
    val responseReader = responseReader(
        mapOf(
            "successFieldResponseName" to listOf("value1", "value2", "value3"),
            "classCastExceptionField" to "anything"
        )
    )

    assertEquals(
        expected = listOf("value1", "value2", "value3"),
        actual = responseReader.readList<Any>(successField) { reader: ResponseReader.ListItemReader -> reader.readString() }
    )
  }

  @Test
  fun readIntList() {
    val successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions)
    val responseReader = responseReader(
        mapOf(
            "successFieldResponseName" to listOf(BigDecimal(1), BigDecimal(2), BigDecimal(3)),
            "classCastExceptionField" to "anything"
        )
    )

    assertEquals(
        expected = listOf(1, 2, 3),
        actual = responseReader.readList<Any>(successField) { reader: ResponseReader.ListItemReader -> reader.readInt() }
    )
  }

  @Test
  fun readLongList() {
    val successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions)
    val responseReader = responseReader(
        mapOf(
            "successFieldResponseName" to listOf(BigDecimal(1), BigDecimal(2), BigDecimal(3)),
            "classCastExceptionField" to "anything"
        )
    )

    assertEquals(
        expected = listOf(1, 2, 3),
        actual = responseReader.readList<Any>(successField) { reader: ResponseReader.ListItemReader -> reader.readInt() }
    )
  }

  @Test
  fun readDoubleList() {
    val successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions)

    val recordSet: MutableMap<String, Any> = HashMap()
    recordSet["successFieldResponseName"] = listOf(BigDecimal(1), BigDecimal(2), BigDecimal(3))
    recordSet["successFieldName"] = listOf(BigDecimal(4), BigDecimal(5))
    recordSet["classCastExceptionField"] = "anything"

    val responseReader = responseReader(
        mapOf(
            "successFieldResponseName" to listOf(BigDecimal(1), BigDecimal(2), BigDecimal(3)),
            "classCastExceptionField" to "anything"
        )
    )

    assertEquals(
        expected = listOf(1.0, 2.0, 3.0),
        actual = responseReader.readList<Any>(successField) { reader: ResponseReader.ListItemReader -> reader.readDouble() }
    )
  }

  @Test
  fun readBooleanList() {
    val successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions)
    val responseReader = responseReader(
        mapOf(
            "successFieldResponseName" to listOf(true, false, true),
            "classCastExceptionField" to "anything"
        )
    )

    assertEquals(
        expected = listOf(true, false, true),
        actual = responseReader.readList<Any>(successField) { reader: ResponseReader.ListItemReader -> reader.readBoolean() }
    )
  }

  @Test
  fun readObjectList() {
    val successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions)
    val responseObject1 = Any()
    val responseObject2 = Any()
    val responseObject3 = Any()
    val objects = listOf(responseObject1, responseObject2, responseObject3)

    val responseReader = responseReader(
        mapOf(
            "successFieldResponseName" to listOf<Map<String, Any>>(emptyMap(), emptyMap(), emptyMap()),
            "classCastExceptionField" to "anything"
        )
    )

    var index = 0
    assertEquals(
        expected = objects,
        actual = responseReader.readList(successField) { reader ->
          reader.readObject { objects[index++] }
        }
    )
  }

  @Test
  fun readListOfScalarList() {
    val successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions)
    val responseReader = responseReader(
        mapOf(
            "successFieldResponseName" to listOf(listOf("1", "2"), listOf("3", "4", "5")),
            "classCastExceptionField" to "anything"
        )
    )

    assertEquals(
        expected = listOf(listOf("1", "2"), listOf("3", "4", "5")),
        actual = responseReader.readList(successField) { reader -> reader.readList(ResponseReader.ListItemReader::readString) }
    )
  }

  @Test
  fun missingFields() {
    val responseReader = responseReader(emptyMap())
    try {
      responseReader.readString(ResponseField.forString("stringField", "stringField", null, false, noConditions))
      fail("expected JsonDataException")
    } catch (expected: JsonDataException) {
      //expected
    }
    try {
      responseReader.readInt(ResponseField.forInt("intField", "intField", null, false, noConditions))
      fail("expected JsonDataException")
    } catch (expected: JsonDataException) {
      //expected
    }
    try {
      responseReader.readDouble(ResponseField.forDouble("doubleField", "doubleField", null, false, noConditions))
      fail("expected JsonDataException")
    } catch (expected: JsonDataException) {
      //expected
    }
    try {
      responseReader.readBoolean(ResponseField.forBoolean("booleanField", "booleanField", null, false, noConditions))
      fail("expected JsonDataException")
    } catch (expected: JsonDataException) {
      //expected
    }
    try {
      responseReader.readObject(ResponseField.forObject("objectField", "objectField", null, false, noConditions)) { null!! }
      fail("expected JsonDataException")
    } catch (expected: JsonDataException) {
      //expected
    }
    try {
      responseReader.readList(ResponseField.forList("scalarListField", "scalarListField", null, false, noConditions)) { null!! }
      fail("expected JsonDataException")
    } catch (expected: JsonDataException) {
      //expected
    }
  }

  @Test
  fun readScalarListWithNulls() {
    val scalarList = ResponseField.forList("list", "list", null, false, noConditions)

    val responseReader = responseReader(
        mapOf(
            "list" to listOf(null, "item2", "item3", null, "item5", null)
        )
    )

    assertEquals(
        expected = listOf(null, "item2", "item3", null, "item5", null),
        actual = responseReader.readList(scalarList) { reader: ResponseReader.ListItemReader -> reader.readString() })
  }

  @Test
  fun readObjectListWithNulls() {
    val listField = ResponseField.forList("list", "list", null, false, noConditions)
    val indexField = ResponseField.forList("index", "index", null, false, noConditions)
    val responseObjects = listOf(null, Any(), Any(), null, Any(), null)

    val responseReader = responseReader(
        mapOf(
            "list" to listOf(
                null,
                mapOf("index" to "1"),
                mapOf("index" to "2"),
                null,
                mapOf("index" to "4"),
                null
            )
        )
    )

    assertEquals(
        expected = responseObjects,
        actual = responseReader.readList(listField) { listReader ->
          listReader.readObject { reader ->
            responseObjects[reader.readString(indexField)!!.toInt()]!!
          }
        }
    )
  }

  companion object {
    private fun responseReader(recordSet: Map<String, Any>): StreamResponseReader {
      val customScalarTypeAdapters: MutableMap<ScalarType, CustomScalarTypeAdapter<*>> = HashMap()
      customScalarTypeAdapters[OBJECT_CUSTOM_TYPE] = object : CustomScalarTypeAdapter<Any?> {
        override fun decode(jsonElement: JsonElement): Any {
          return jsonElement.toRawValue().toString()
        }

        override fun encode(value: Any?): JsonElement {
          throw UnsupportedOperationException()
        }
      }
      val jsonReader = BufferedSourceJsonReader(
          Buffer().apply {
            Utils.writeToJson(
                value = recordSet,
                jsonWriter = JsonUtf8Writer(this),
            )
            flush()
          }
      ).beginObject()
      return StreamResponseReader(
          jsonReader = jsonReader,
          variables = EMPTY_OPERATION.variables(),
          scalarTypeAdapters = ScalarTypeAdapters(customScalarTypeAdapters),
      )
    }

    private fun scalarTypeFor(clazz: KClass<*>, className: String): ScalarType {
      return object : ScalarType {
        override val graphqlName
          get() = clazz.simpleName!!

        override val className = className
      }
    }

    private val OBJECT_CUSTOM_TYPE: ScalarType = object : ScalarType {
      override val graphqlName = "CustomObject"

      override val className = "kotlin.String"
    }
  }
}
