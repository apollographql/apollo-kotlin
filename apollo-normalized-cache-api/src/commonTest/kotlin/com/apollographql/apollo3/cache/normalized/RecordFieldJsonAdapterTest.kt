package com.apollographql.apollo.cache.normalized

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecordFieldJsonAdapterTest {

  @Test
  fun testFieldsAdapterSerializationDeserialization() {
    val expectedDouble = "1.23"
    val expectedStringValue = "StringValue"
    val expectedBooleanValue = true
    val expectedCacheReference = CacheReference("foo")
    val expectedCacheReferenceList = listOf(CacheReference("bar"), CacheReference("baz"))
    val expectedScalarList = listOf("scalarOne", "scalarTwo")
    val expectedListOfScalarList = listOf(listOf("scalarOne", "scalarTwo"))
    val expectedMapKey = "foo"
    val expectedMapValue = "bar"
    val expectedMap = mapOf(expectedMapKey to expectedMapValue)
    val record = Record(
        key = "root",
        fields = mapOf(
            "double" to expectedDouble,
            "string" to expectedStringValue,
            "boolean" to expectedBooleanValue,
            "cacheReference" to expectedCacheReference,
            "scalarList" to expectedScalarList,
            "referenceList" to expectedCacheReferenceList,
            "nullValue" to null,
            "listOfScalarList" to expectedListOfScalarList,
            "map" to expectedMap
        )
    )

    val json = RecordFieldJsonAdapter.toJson(record.fields)
    val deserializedMap = requireNotNull(RecordFieldJsonAdapter.fromJson(json))

    assertEquals(actual = deserializedMap["double"], expected = expectedDouble)
    assertEquals(actual = deserializedMap["string"], expected = expectedStringValue)
    assertEquals(actual = deserializedMap["boolean"], expected = expectedBooleanValue)
    assertEquals(actual = deserializedMap["cacheReference"], expected = expectedCacheReference)
    assertEquals(actual = deserializedMap["scalarList"], expected = expectedScalarList)
    assertEquals(actual = deserializedMap["referenceList"], expected = expectedCacheReferenceList)
    assertTrue { deserializedMap.containsKey("nullValue") }
    assertNull(deserializedMap["nullValue"])
    assertEquals(actual = (deserializedMap["listOfScalarList"] as List<*>).size, expected = 1)
    assertEquals(
        actual = (deserializedMap["listOfScalarList"] as List<*>)[0] as Iterable<*>?,
        expected = expectedScalarList)
    assertEquals(actual = (deserializedMap["map"] as Map<*, *>)[expectedMapKey], expected = expectedMapValue)
  }
}
