package com.apollographql.apollo.cache.normalized.sql

import com.apollographql.apollo.cache.ApolloCacheHeaders
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SqlNormalizedCacheTest {

  private val cache: SqlNormalizedCache = SqlNormalizedCacheFactory(createDriver()).create(RecordFieldJsonAdapter())

  @BeforeTest
  fun setUp() {
    cache.clearAll()
  }

  @Test
  fun testRecordCreation() {
    createRecord(STANDARD_KEY)
    assertNotNull(cache.loadRecord(STANDARD_KEY, CacheHeaders.NONE))
  }

  @Test
  fun testRecordCreation_root() {
    createRecord(QUERY_ROOT_KEY)
    assertNotNull(cache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE))
  }

  @Test
  fun testRecordSelection() {
    createRecord(STANDARD_KEY)
    val record = cache.selectRecordForKey(STANDARD_KEY)
    assertNotNull(record)
    assertEquals(expected = STANDARD_KEY, actual = record.key)
  }

  @Test
  fun testRecordSelection_root() {
    createRecord(QUERY_ROOT_KEY)
    val record = requireNotNull(cache.selectRecordForKey(QUERY_ROOT_KEY))
    assertNotNull(record)
    assertEquals(expected = QUERY_ROOT_KEY, actual = record.key)
  }

  @Test
  fun testRecordSelection_recordNotPresent() {
    val record = cache.loadRecord(STANDARD_KEY, CacheHeaders.NONE)
    assertNull(record)
  }

  @Test
  fun testRecordMerge() {
    cache.merge(Record.builder(STANDARD_KEY)
        .addField("fieldKey", "valueUpdated")
        .addField("newFieldKey", true).build(), CacheHeaders.NONE)
    val record = cache.selectRecordForKey(STANDARD_KEY)
    assertNotNull(record)
    assertEquals(expected = "valueUpdated", actual = record.fields["fieldKey"])
    assertEquals(expected = true, actual = record.fields["newFieldKey"])
  }

  @Test
  fun testRecordDelete() {
    createRecord(STANDARD_KEY)
    cache.merge(Record.builder(STANDARD_KEY)
        .addField("fieldKey", "valueUpdated")
        .addField("newFieldKey", true).build(), CacheHeaders.NONE)
    cache.deleteRecord(STANDARD_KEY)
    val record = cache.selectRecordForKey(STANDARD_KEY)
    assertNull(record)
  }

  @Test
  fun testClearAll() {
    createRecord(QUERY_ROOT_KEY)
    createRecord(STANDARD_KEY)
    cache.clearAll()
    assertNull(cache.selectRecordForKey(QUERY_ROOT_KEY))
    assertNull(cache.selectRecordForKey(STANDARD_KEY))
  }

  // Tests for StandardCacheHeader compliance
  @Test
  fun testHeader_evictAfterRead() {
    createRecord(STANDARD_KEY)
    val record = cache.loadRecord(STANDARD_KEY, CacheHeaders.builder()
        .addHeader(ApolloCacheHeaders.EVICT_AFTER_READ, "true").build())
    assertNotNull(record)
    val nullRecord = cache.loadRecord(STANDARD_KEY, CacheHeaders.builder()
        .addHeader(ApolloCacheHeaders.EVICT_AFTER_READ, "true").build())
    assertNull(nullRecord)
  }

  @Test
  fun testHeader_noCache() {
    cache.merge(Record.builder(STANDARD_KEY).build(),
        CacheHeaders.builder().addHeader(ApolloCacheHeaders.DO_NOT_STORE, "true").build())
    val record = cache.loadRecord(STANDARD_KEY, CacheHeaders.NONE)
    assertNull(record)
  }

  @Test
  fun testRecordMerge_noOldRecord() {
    cache.merge(Record.builder(STANDARD_KEY)
        .addField("fieldKey", "valueUpdated")
        .addField("newFieldKey", true).build(), CacheHeaders.NONE)
    val record = cache.selectRecordForKey(STANDARD_KEY)
    assertNotNull(record)
    assertEquals(expected = "valueUpdated", actual = record.fields["fieldKey"])
    assertEquals(expected = true, actual = record.fields["newFieldKey"])
  }

  @Test
  fun testRecordMerge_withOldRecord() {
    createRecord(STANDARD_KEY)
    cache.merge(Record.builder(STANDARD_KEY)
        .addField("fieldKey", "valueUpdated")
        .addField("newFieldKey", true).build(), CacheHeaders.NONE)
    val record = cache.selectRecordForKey(STANDARD_KEY)
    assertNotNull(record)
    assertEquals(expected = "valueUpdated", actual = record.fields["fieldKey"])
    assertEquals(expected = true, actual = record.fields["newFieldKey"])
  }

  private fun createRecord(key: String) {
    cache.createRecord(key, FIELDS)
  }

  companion object {
    const val STANDARD_KEY = "key"
    const val QUERY_ROOT_KEY = "QUERY_ROOT"
    const val FIELDS = "{\"fieldKey\": \"value\"}"
  }
}
