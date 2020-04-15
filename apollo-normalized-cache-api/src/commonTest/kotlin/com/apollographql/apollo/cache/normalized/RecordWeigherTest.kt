package com.apollographql.apollo.cache.normalized

import com.apollographql.apollo.api.BigDecimal
import com.apollographql.apollo.cache.normalized.Record.Companion.builder
import kotlin.test.Test
import kotlin.test.assertNotEquals

class RecordWeigherTest {

  @Test
  fun testRecordWeigher() {
    val recordBuilder = builder("root")
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

    //It's difficult to say what the "right" size estimate is, so just checking it is has been calculate at all.
    assertNotEquals(actual = record.sizeEstimateBytes(), illegal = -1)
  }
}
