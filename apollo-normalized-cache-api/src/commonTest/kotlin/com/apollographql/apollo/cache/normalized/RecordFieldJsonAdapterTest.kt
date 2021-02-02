package com.apollographql.apollo.cache.normalized

import com.apollographql.apollo.api.BigDecimal
import com.apollographql.apollo.cache.normalized.Record.Companion.builder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecordFieldJsonAdapterTest {

  @Test
  fun testFieldsAdapterSerializationDeserialization() {
    val recordBuilder = builder("root")
    val expectedBigDecimal = BigDecimal("1.23")
    val expectedStringValue = "StringValue"
    val expectedBooleanValue = true
    val expectedCacheReference = CacheReference("foo")
    val expectedCacheReferenceList = listOf(CacheReference("bar"), CacheReference("baz"))
    val expectedScalarList = listOf("scalarOne", "scalarTwo")
    val expectedListOfScalarList = listOf(listOf("scalarOne", "scalarTwo"))
    val expectedMapKey = "foo"
    val expectedMapValue = "bar"
    val expectedMap = mapOf(expectedMapKey to expectedMapValue)
    recordBuilder.addField("bigDecimal", expectedBigDecimal)
    recordBuilder.addField("string", expectedStringValue)
    recordBuilder.addField("boolean", expectedBooleanValue)
    recordBuilder.addField("cacheReference", expectedCacheReference)
    recordBuilder.addField("scalarList", expectedScalarList)
    recordBuilder.addField("referenceList", expectedCacheReferenceList)
    recordBuilder.addField("nullValue", null)
    recordBuilder.addField("listOfScalarList", expectedListOfScalarList)
    recordBuilder.addField("map", expectedMap)
    val record = recordBuilder.build()
    val json = RecordFieldJsonAdapter.toJson(record.fields)
    val deserializedMap = requireNotNull(RecordFieldJsonAdapter.fromJson(json))

    assertEquals(actual = deserializedMap["bigDecimal"], expected = expectedBigDecimal)
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
