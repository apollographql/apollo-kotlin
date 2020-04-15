package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.BigDecimal
import com.apollographql.apollo.api.CustomTypeAdapter
import com.apollographql.apollo.api.CustomTypeValue
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseField.Condition.Companion.typeCondition
import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.api.ScalarTypeAdapters
import okio.BufferedSource
import okio.ByteString
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class SimpleResponseReaderTest {
  private val noConditions: List<ResponseField.Condition> = emptyList()

  @Test
  fun readString() {
    val successField = ResponseField.forString("successFieldResponseName", "successFieldName", null, false, noConditions)
    val classCastExceptionField = ResponseField.forString("classCastExceptionField", "classCastExceptionField", null, false,
        noConditions)
    val recordSet: MutableMap<String, Any> = HashMap()
    recordSet["successFieldResponseName"] = "response1"
    recordSet["successFieldName"] = "response2"
    recordSet["classCastExceptionField"] = 1
    val responseReader = responseReader(recordSet)
    assertEquals("response1", responseReader.readString(successField))
    try {
      responseReader.readString(classCastExceptionField)
      fail("expected ClassCastException")
    } catch (expected: ClassCastException) {
      // expected
    }
  }

  @Test
  fun readInt() {
    val successField = ResponseField.forInt("successFieldResponseName", "successFieldName", null, false, noConditions)
    val classCastExceptionField = ResponseField.forInt("classCastExceptionField", "classCastExceptionField", null, false,
        noConditions)
    val recordSet: MutableMap<String, Any> = HashMap()
    recordSet["successFieldResponseName"] = BigDecimal(1)
    recordSet["successFieldName"] = BigDecimal(2)
    recordSet["classCastExceptionField"] = "anything"
    val responseReader = responseReader(recordSet)
    assertEquals(1, responseReader.readInt(successField))
    try {
      responseReader.readInt(classCastExceptionField)
      fail("expected ClassCastException")
    } catch (expected: ClassCastException) {
      // expected
    }
  }

  @Test
  fun readLong() {
    val successField = ResponseField.forLong("successFieldResponseName", "successFieldName", null, false, noConditions)
    val classCastExceptionField = ResponseField.forLong("classCastExceptionField", "classCastExceptionField", null, false,
        noConditions)
    val recordSet: MutableMap<String, Any> = HashMap()
    recordSet["successFieldResponseName"] = BigDecimal(1)
    recordSet["successFieldName"] = BigDecimal(2)
    recordSet["classCastExceptionField"] = "anything"
    val responseReader = responseReader(recordSet)
    assertEquals(1, responseReader.readLong(successField) as Long)
    try {
      responseReader.readLong(classCastExceptionField)
      fail("expected ClassCastException")
    } catch (expected: ClassCastException) {
      // expected
    }
  }

  @Test
  fun readDouble() {
    val successField = ResponseField.forDouble("successFieldResponseName", "successFieldName", null, false, noConditions)
    val classCastExceptionField = ResponseField.forDouble("classCastExceptionField", "classCastExceptionField", null, false,
        noConditions)
    val recordSet: MutableMap<String, Any> = HashMap()
    recordSet["successFieldResponseName"] = BigDecimal(1.1)
    recordSet["successFieldName"] = BigDecimal(2.2)
    recordSet["classCastExceptionField"] = "anything"
    val responseReader = responseReader(recordSet)
    assertEquals(1.1, responseReader.readDouble(successField)!!)
    try {
      responseReader.readDouble(classCastExceptionField)
      fail("expected ClassCastException")
    } catch (expected: ClassCastException) {
      // expected
    }
  }

  @Test
  fun readBoolean() {
    val successField = ResponseField.forBoolean("successFieldResponseName", "successFieldName", null, false, noConditions)
    val classCastExceptionField = ResponseField.forBoolean("classCastExceptionField", "classCastExceptionField", null,
        false, noConditions)
    val recordSet: MutableMap<String, Any> = HashMap()
    recordSet["successFieldResponseName"] = true
    recordSet["successFieldName"] = false
    recordSet["classCastExceptionField"] = "anything"
    val responseReader = responseReader(recordSet)
    assertTrue(responseReader.readBoolean(successField)!!)
    try {
      responseReader.readBoolean(classCastExceptionField)
      fail("expected ClassCastException")
    } catch (expected: ClassCastException) {
      // expected
    }
  }

  @Test
  fun readObject() {
    val responseObject = Any()
    val successField = ResponseField.forObject("successFieldResponseName", "successFieldName", null, false, noConditions)
    val classCastExceptionField = ResponseField.forObject("classCastExceptionField", "classCastExceptionField", null, false,
        noConditions)
    val recordSet: MutableMap<String, Any> = HashMap()
    recordSet["successFieldResponseName"] = emptyMap<Any, Any>()
    recordSet["successFieldName"] = emptyMap<Any, Any>()
    recordSet["classCastExceptionField"] = "anything"
    val responseReader = responseReader(recordSet)
    assertEquals(responseObject, responseReader.readObject(successField, object : ResponseReader.ObjectReader<Any> {
      override fun read(reader: ResponseReader): Any {
        return responseObject
      }
    }))
    try {
      responseReader.readObject(classCastExceptionField, object : ResponseReader.ObjectReader<Any> {
        override fun read(reader: ResponseReader): Any {
          return reader.readString(ResponseField.forString("anything", "anything", null, true, noConditions))!!
        }
      })
      fail("expected ClassCastException")
    } catch (expected: ClassCastException) {
      // expected
    }
  }

  @Test
  fun readFragment() {
    val responseObject = Any()
    val successFragmentField = ResponseField.forFragment("__typename", "__typename", listOf<ResponseField.Condition>(typeCondition(arrayOf("Fragment1"))))
    val skipFragmentField = ResponseField.forFragment("__typename", "__typename", listOf<ResponseField.Condition>(typeCondition(arrayOf("Fragment2"))))
    val recordSet: MutableMap<String, Any> = HashMap()
    recordSet["__typename"] = "Fragment1"
    val responseReader = responseReader(recordSet)
    assertEquals(responseObject, responseReader.readFragment(successFragmentField, object : ResponseReader.ObjectReader<Any> {
      override fun read(reader: ResponseReader): Any {
        return responseObject
      }
    }))
    assertNull(responseReader.readFragment(skipFragmentField, object : ResponseReader.ObjectReader<Any> {
      override fun read(reader: ResponseReader): Any {
        return responseObject
      }
    }))
  }

  @Test
  fun readCustomObjectMap() {
    val mapScalarType: ScalarType = object : ScalarType {
      override fun typeName(): String {
        return Map::class.simpleName!!
      }

      override fun className(): String {
        return Map::class.qualifiedName!!
      }
    }
    val successField = ResponseField.forCustomType("successFieldResponseName", "successFieldName", null,
        false, mapScalarType, noConditions)
    val objectMap = mutableMapOf<String, Any>()
    objectMap["string"] = "string"
    objectMap["boolean"] = true
    objectMap["double"] = 1.99
    objectMap["float"] = 2.99f
    objectMap["long"] = 3L
    objectMap["int"] = 4
    objectMap["stringList"] = listOf("string1", "string2")
    objectMap["booleanList"] = listOf("true", "false")
    objectMap["doubleList"] = listOf(1.99, 2.99)
    objectMap["floatList"] = listOf(3.99f, 4.99f, 5.99f)
    objectMap["longList"] = listOf(5L, 7L)
    objectMap["intList"] = listOf(8, 9, 10)
    objectMap["object"] = HashMap(objectMap)
    objectMap["objectList"] = listOf(HashMap(objectMap), HashMap(objectMap))
    val recordSet: MutableMap<String, Any> = HashMap()
    recordSet["successFieldResponseName"] = objectMap
    recordSet["successFieldName"] = objectMap
    val responseReader = responseReader(recordSet)
    assertEquals(objectMap, responseReader.readCustomType<Map<*, *>>(successField))
  }

  @Test
  fun readCustomObjectList() {
    val listScalarType: ScalarType = object : ScalarType {
      override fun typeName(): String {
        return List::class.simpleName!!
      }

      override fun className(): String {
        return List::class.qualifiedName!!
      }
    }
    val successField = ResponseField.forCustomType("successFieldResponseName", "successFieldName", null,
        false, listScalarType, noConditions)
    val objectMap: MutableMap<String, Any> = HashMap()
    objectMap["string"] = "string"
    objectMap["boolean"] = true
    val objectList = listOf<Map<String, Any>>(objectMap, objectMap)
    val recordSet: MutableMap<String, Any> = HashMap()
    recordSet["successFieldResponseName"] = objectList
    recordSet["successFieldName"] = objectList
    val responseReader = responseReader(recordSet)
    assertEquals(objectList, responseReader.readCustomType<List<*>>(successField))
  }

  @Test
  fun readCustomWithDefaultAdapter() {
    val stringField = ResponseField.forCustomType("stringField", "stringField", null, false,
        scalarTypeFor(String::class), noConditions)
    val booleanField = ResponseField.forCustomType("booleanField", "booleanField", null, false,
        scalarTypeFor(Boolean::class), noConditions)
    val integerField = ResponseField.forCustomType("integerField", "integerField", null, false,
        scalarTypeFor(Int::class), noConditions)
    val longField = ResponseField.forCustomType("longField", "longField", null, false,
        scalarTypeFor(Long::class), noConditions)
    val floatField = ResponseField.forCustomType("floatField", "floatField", null, false,
        scalarTypeFor(Float::class), noConditions)
    val doubleField = ResponseField.forCustomType("doubleField", "doubleField", null, false,
        scalarTypeFor(Double::class), noConditions)
    val unsupportedField = ResponseField.forCustomType("unsupportedField", "unsupportedField", null, false,
        scalarTypeFor(RuntimeException::class), noConditions)
    val recordSet: MutableMap<String, Any> = HashMap()
    recordSet[stringField.responseName] = "string"
    recordSet[booleanField.responseName] = true
    recordSet[integerField.responseName] = BigDecimal(1)
    recordSet[longField.responseName] = BigDecimal(2)
    recordSet[floatField.responseName] = BigDecimal("3.99")
    recordSet[doubleField.responseName] = BigDecimal("4.99")
    recordSet[unsupportedField.responseName] = "smth"
    val responseReader = responseReader(recordSet)
    assertEquals("string", responseReader.readCustomType(stringField)!!)
    assertEquals(true, responseReader.readCustomType(booleanField)!!)
    assertEquals(1, responseReader.readCustomType(integerField)!!)
    assertEquals(2L, responseReader.readCustomType(longField)!!)
    assertEquals(3.99f, responseReader.readCustomType(floatField)!!)
    assertEquals(4.99, responseReader.readCustomType(doubleField)!!)
    try {
      responseReader.readCustomType<Any>(unsupportedField)
      fail("Expect IllegalArgumentException")
    } catch (expected: IllegalArgumentException) {
      // expected
    }
  }

  @Test
  fun readStringList() {
    val successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions)
    val recordSet: MutableMap<String, Any> = HashMap()
    recordSet["successFieldResponseName"] = listOf("value1", "value2", "value3")
    recordSet["successFieldName"] = listOf("value4", "value5")
    recordSet["classCastExceptionField"] = "anything"
    val responseReader = responseReader(recordSet)
    assertEquals(
        listOf("value1", "value2", "value3"),
        responseReader.readList<Any>(successField) { reader: ResponseReader.ListItemReader -> reader.readString() }
    )
  }

  @Test
  fun readIntList() {
    val successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions)
    val recordSet: MutableMap<String, Any> = HashMap()
    recordSet["successFieldResponseName"] = listOf(BigDecimal(1), BigDecimal(2), BigDecimal(3))
    recordSet["successFieldName"] = listOf(BigDecimal(4), BigDecimal(5))
    recordSet["classCastExceptionField"] = "anything"
    val responseReader = responseReader(recordSet)
    assertEquals(
        listOf(1, 2, 3),
        responseReader.readList<Any>(successField) { reader: ResponseReader.ListItemReader -> reader.readInt() }
    )
  }

  @Test
  fun readLongList() {
    val successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions)
    val recordSet: MutableMap<String, Any> = HashMap()
    recordSet["successFieldResponseName"] = listOf(BigDecimal(1), BigDecimal(2), BigDecimal(3))
    recordSet["successFieldName"] = listOf(BigDecimal(4), BigDecimal(5))
    recordSet["classCastExceptionField"] = "anything"
    val responseReader = responseReader(recordSet)
    assertEquals(
        listOf(1, 2, 3),
        responseReader.readList<Any>(successField) { reader: ResponseReader.ListItemReader -> reader.readInt() }
    )
  }

  @Test
  fun readDoubleList() {
    val successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions)
    val recordSet: MutableMap<String, Any> = HashMap()
    recordSet["successFieldResponseName"] = listOf(BigDecimal(1), BigDecimal(2), BigDecimal(3))
    recordSet["successFieldName"] = listOf(BigDecimal(4), BigDecimal(5))
    recordSet["classCastExceptionField"] = "anything"
    val responseReader = responseReader(recordSet)
    assertEquals(
        listOf(1.0, 2.0, 3.0),
        responseReader.readList<Any>(successField) { reader: ResponseReader.ListItemReader -> reader.readDouble() }
    )
  }

  @Test
  fun readBooleanList() {
    val successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions)
    val recordSet: MutableMap<String, Any> = HashMap()
    recordSet["successFieldResponseName"] = listOf(true, false, true)
    recordSet["successFieldName"] = listOf(false, false)
    recordSet["classCastExceptionField"] = "anything"
    val responseReader = responseReader(recordSet)
    assertEquals(
        listOf(true, false, true),
        responseReader.readList<Any>(successField) { reader: ResponseReader.ListItemReader -> reader.readBoolean() }
    )
  }

  @Test
  fun readObjectList() {
    val successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions)
    val responseObject1 = Any()
    val responseObject2 = Any()
    val responseObject3 = Any()
    val objects = listOf(responseObject1, responseObject2, responseObject3)
    val recordSet: MutableMap<String, Any> = HashMap()
    recordSet["successFieldResponseName"] = listOf(emptyMap(), emptyMap(), emptyMap<Any, Any>())
    recordSet["successFieldName"] = listOf(emptyMap(), emptyMap(), emptyMap<Any, Any>())
    recordSet["classCastExceptionField"] = "anything"
    val responseReader = responseReader(recordSet)

    var index = 0
    assertEquals(objects, responseReader.readList(successField) { reader ->
      reader.readObject { objects[index++] }
    }
    )
  }

  @Test
  fun readListOfScalarList() {
    val successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions)
    val response1 = listOf(listOf("1", "2"), listOf("3", "4", "5"))
    val response2 = listOf(listOf("6", "7", "8"), listOf("9", "0"))
    val recordSet: MutableMap<String, Any> = HashMap()
    recordSet["successFieldResponseName"] = response1
    recordSet["successFieldName"] = response2
    recordSet["classCastExceptionField"] = "anything"
    val responseReader = responseReader(recordSet)
    assertEquals(
        listOf(listOf("1", "2"), listOf("3", "4", "5")),
        responseReader.readList(successField) { reader -> reader.readList(ResponseReader.ListItemReader::readString) }
    )
  }

  @Test
  fun optionalFieldsIOException() {
    val responseReader = responseReader(emptyMap())
    responseReader.readString(ResponseField.forString("stringField", "stringField", null, true, noConditions))
    responseReader.readInt(ResponseField.forInt("intField", "intField", null, true, noConditions))
    responseReader.readLong(ResponseField.forLong("longField", "longField", null, true, noConditions))
    responseReader.readDouble(ResponseField.forDouble("doubleField", "doubleField", null, true, noConditions))
    responseReader.readBoolean(ResponseField.forBoolean("booleanField", "booleanField", null, true, noConditions))
    responseReader.readObject(ResponseField.forObject("objectField", "objectField", null, true, noConditions)) { null!! }
    responseReader.readList(ResponseField.forList("scalarListField", "scalarListField", null, true, noConditions)) { null!! }
  }

  @Test
  fun mandatoryFieldsIOException() {
    val responseReader = responseReader(emptyMap())
    try {
      responseReader.readString(ResponseField.forString("stringField", "stringField", null, false, noConditions))
      fail("expected NullPointerException")
    } catch (expected: NullPointerException) {
      //expected
    }
    try {
      responseReader.readInt(ResponseField.forInt("intField", "intField", null, false, noConditions))
      fail("expected NullPointerException")
    } catch (expected: NullPointerException) {
      //expected
    }
    try {
      responseReader.readLong(ResponseField.forLong("longField", "longField", null, false, noConditions))
      fail("expected NullPointerException")
    } catch (expected: NullPointerException) {
      //expected
    }
    try {
      responseReader.readDouble(ResponseField.forDouble("doubleField", "doubleField", null, false, noConditions))
      fail("expected NullPointerException")
    } catch (expected: NullPointerException) {
      //expected
    }
    try {
      responseReader.readBoolean(ResponseField.forBoolean("booleanField", "booleanField", null, false, noConditions))
      fail("expected NullPointerException")
    } catch (expected: NullPointerException) {
      //expected
    }
    try {
      responseReader.readObject(ResponseField.forObject("objectField", "objectField", null, false, noConditions)) { null!! }
      fail("expected NullPointerException")
    } catch (expected: NullPointerException) {
      //expected
    }
    try {
      responseReader.readList(ResponseField.forList("scalarListField", "scalarListField", null, false, noConditions)) { null!! }
      fail("expected NullPointerException")
    } catch (expected: NullPointerException) {
      //expected
    }
  }

  @Test
  fun readScalarListWithNulls() {
    val scalarList = ResponseField.forList("list", "list", null, false, noConditions)
    val recordSet: MutableMap<String, Any> = HashMap()
    recordSet["list"] = listOf(null, "item2", "item3", null, "item5", null)
    val responseReader = responseReader(recordSet)
    assertEquals(
        listOf(null, "item2", "item3", null, "item5", null),
        responseReader.readList(scalarList) { reader: ResponseReader.ListItemReader -> reader.readString() })
  }

  @Test
  fun readObjectListWithNulls() {
    val listField = ResponseField.forList("list", "list", null, false, noConditions)
    val indexField = ResponseField.forList("index", "index", null, false, noConditions)
    val responseObjects = listOf(null, Any(), Any(), null, Any(), null)
    val recordSet: MutableMap<String, Any> = HashMap()
    recordSet["list"] = listOf(
        null,
        mapOf("index" to "1"),
        mapOf("index" to "2"),
        null,
        mapOf("index" to "4"),
        null
    )
    val responseReader = responseReader(recordSet)
    assertEquals(
        responseObjects,
        responseReader.readList(listField) { listReader ->
          listReader.readObject { reader ->
            responseObjects[reader.readString(indexField)!!.toInt()]!!
          }
        }
    )
  }

  companion object {
    private fun responseReader(recordSet: Map<String, Any>): SimpleResponseReader {
      val customTypeAdapters: MutableMap<ScalarType, CustomTypeAdapter<*>> = HashMap()
      customTypeAdapters[OBJECT_CUSTOM_TYPE] = object : CustomTypeAdapter<Any?> {
        override fun decode(value: CustomTypeValue<*>): Any {
          return value.value.toString()
        }

        override fun encode(value: Any?): CustomTypeValue<*> {
          throw UnsupportedOperationException()
        }
      }
      return SimpleResponseReader(recordSet, EMPTY_OPERATION.variables(), ScalarTypeAdapters(customTypeAdapters))
    }

    private fun scalarTypeFor(clazz: KClass<*>): ScalarType {
      return object : ScalarType {
        override fun typeName(): String {
          return clazz.simpleName!!
        }

        override fun className(): String {
          return clazz.qualifiedName!!
        }
      }
    }

    private val OBJECT_CUSTOM_TYPE: ScalarType = object : ScalarType {
      override fun typeName(): String {
        return String::class.simpleName!!
      }

      override fun className(): String {
        return String::class.qualifiedName!!
      }
    }
    private val EMPTY_OPERATION: Operation<*, *, *> = object : Operation<Operation.Data, Any?, Operation.Variables> {
      override fun queryDocument(): String {
        throw UnsupportedOperationException()
      }

      override fun variables(): Operation.Variables {
        return Operation.EMPTY_VARIABLES
      }

      override fun responseFieldMapper(): ResponseFieldMapper<Operation.Data> {
        throw UnsupportedOperationException()
      }

      override fun wrapData(data: Operation.Data?): Any? {
        throw UnsupportedOperationException()
      }

      override fun name(): OperationName {
        return object : OperationName {
          override fun name(): String {
            return "test"
          }
        }
      }

      override fun operationId(): String {
        return ""
      }

      override fun parse(source: BufferedSource): Response<Any?> {
        throw UnsupportedOperationException()
      }

      override fun parse(source: BufferedSource, scalarTypeAdapters: ScalarTypeAdapters): Response<Any?> {
        throw UnsupportedOperationException()
      }

      override fun composeRequestBody(): ByteString {
        throw UnsupportedOperationException()
      }

      override fun composeRequestBody(scalarTypeAdapters: ScalarTypeAdapters): ByteString {
        throw UnsupportedOperationException()
      }
    }
  }
}
