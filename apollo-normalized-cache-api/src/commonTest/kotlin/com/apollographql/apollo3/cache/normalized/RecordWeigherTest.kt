package com.apollographql.apollo3.cache.normalized

import kotlin.test.Test
import kotlin.test.assertTrue

class RecordWeigherTest {

  @Test
  fun testRecordWeigher() {
    val expectedDouble = 1.23
    val expectedStringValue = "StringValue"
    val expectedBooleanValue = true
    val expectedCacheKey = CacheKey("foo")
    val expectedCacheKeyList = listOf(CacheKey("bar"), CacheKey("baz"))
    val expectedScalarList = listOf("scalarOne", "scalarTwo")
    val record = Record(
        key = "root",
        fields = mapOf(
            "double" to expectedDouble,
            "string" to expectedStringValue,
            "boolean" to expectedBooleanValue,
            "cacheReference" to expectedCacheKey,
            "scalarList" to expectedScalarList,
            "referenceList" to expectedCacheKeyList,
        )
    )
    assertTrue(record.sizeInBytes <= 218)
    assertTrue(record.sizeInBytes >= 214) // JS takes less space, maybe for strings?
  }
}
