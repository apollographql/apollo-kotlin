package com.apollographql.apollo.cache.normalized

import com.apollographql.apollo.api.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class RecordWeigherTest {

  @Test
  fun testRecordWeigher() {
    val expectedBigDecimal = BigDecimal(1.23)
    val expectedStringValue = "StringValue"
    val expectedBooleanValue = true
    val expectedCacheReference = CacheReference("foo")
    val expectedCacheReferenceList = listOf(CacheReference("bar"), CacheReference("baz"))
    val expectedScalarList = listOf("scalarOne", "scalarTwo")
    val record = Record(
        key = "root",
        fields = mapOf(
            "bigDecimal" to expectedBigDecimal,
            "string" to expectedStringValue,
            "boolean" to expectedBooleanValue,
            "cacheReference" to expectedCacheReference,
            "scalarList" to expectedScalarList,
            "referenceList" to expectedCacheReferenceList,
        )
    )
    assertEquals(actual = record.sizeInBytes, expected = 246)
  }
}
