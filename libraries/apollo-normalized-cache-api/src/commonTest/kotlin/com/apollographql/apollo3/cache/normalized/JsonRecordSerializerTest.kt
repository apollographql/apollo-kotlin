package com.apollographql.apollo.cache.normalized

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.cache.normalized.api.CacheKey
import com.apollographql.apollo.cache.normalized.api.Record
import com.apollographql.apollo.cache.normalized.api.internal.JsonRecordSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsonRecordSerializerTest {

  @Test
  fun testFieldsAdapterSerializationDeserialization() {
    val expectedDouble = "1.23"
    val expectedStringValue = "StringValue"
    val expectedBooleanValue = true
    val expectedCacheKey = CacheKey("foo")
    val expectedCacheKeyList = listOf(CacheKey("bar"), CacheKey("baz"))
    val expectedScalarList = listOf("scalarOne", "scalarTwo")
    val expectedListOfScalarList = listOf(listOf("scalarOne", "scalarTwo"))
    val expectedMapKey = "foo"
    val expectedMapValue = "bar"
    val expectedMap = mapOf(expectedMapKey to expectedMapValue)
    val expectLongValue: Long = 1
    val record = Record(
        key = "root",
        fields = mapOf(
            "double" to expectedDouble,
            "string" to expectedStringValue,
            "boolean" to expectedBooleanValue,
            "cacheReference" to expectedCacheKey,
            "scalarList" to expectedScalarList,
            "referenceList" to expectedCacheKeyList,
            "nullValue" to null,
            "listOfScalarList" to expectedListOfScalarList,
            "map" to expectedMap,
            "long" to expectLongValue
        )
    )

    val json = JsonRecordSerializer.serialize(record)
    val deserializedMap = requireNotNull(JsonRecordSerializer.deserialize(record.key, json))

    assertEquals(actual = deserializedMap["double"], expected = expectedDouble)
    assertEquals(actual = deserializedMap["string"], expected = expectedStringValue)
    assertEquals(actual = deserializedMap["boolean"], expected = expectedBooleanValue)
    assertEquals(actual = deserializedMap["cacheReference"], expected = expectedCacheKey)
    assertEquals(actual = deserializedMap["scalarList"], expected = expectedScalarList)
    assertEquals(actual = deserializedMap["referenceList"], expected = expectedCacheKeyList)
    assertTrue { deserializedMap.containsKey("nullValue") }
    assertNull(deserializedMap["nullValue"])
    assertEquals(actual = (deserializedMap["listOfScalarList"] as List<*>).size, expected = 1)
    assertEquals(
        actual = (deserializedMap["listOfScalarList"] as List<*>)[0] as Iterable<*>?,
        expected = expectedScalarList)
    assertEquals(actual = (deserializedMap["map"] as Map<*, *>)[expectedMapKey], expected = expectedMapValue)
    // The default deserialization algorithm will use the number type with the smallest possible width.
    // This is OK as the generated parser know what to expect and will convert back to Long if needed.
    // This test compares Strings to avoid a failure.
    assertEquals(actual = deserializedMap["long"]?.toString(), expected = expectLongValue.toString())

  }
}
