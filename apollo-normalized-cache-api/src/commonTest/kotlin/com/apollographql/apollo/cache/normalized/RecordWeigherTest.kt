package com.apollographql.apollo.cache.normalized

import com.apollographql.apollo.api.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class RecordWeigherTest {

  @Test
  fun testRecordWeigher() {
    val recordBuilder = Record.builder("root")
    val expectedBigDecimal = BigDecimal(1.23)
    val expectedStringValue = "StringValue"
    val expectedBooleanValue = true
    val expectedCacheReference = CacheReference("foo")
    val expectedCacheReferenceList = listOf(CacheReference("bar"), CacheReference("baz"))
    val expectedScalarList = listOf("scalarOne", "scalarTwo")
    recordBuilder.addField("bigDecimal", expectedBigDecimal)
    recordBuilder.addField("string", expectedStringValue)
    recordBuilder.addField("boolean", expectedBooleanValue)
    recordBuilder.addField("cacheReference", expectedCacheReference)
    recordBuilder.addField("scalarList", expectedScalarList)
    recordBuilder.addField("referenceList", expectedCacheReferenceList)
    val record = recordBuilder.build()
    record.sizeEstimateBytes()
    
    assertEquals(actual = record.sizeEstimateBytes(), expected = 246)
  }
}
