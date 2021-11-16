package com.apollographql.apollo3.cache.normalized.sql

import com.apollographql.apollo3.cache.normalized.api.ApolloCacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.Record
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlNormalizedCacheTest {

  private val cache: SqlNormalizedCache = SqlNormalizedCacheFactory(createDriver()).create()

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
    val record = cache.loadRecord(STANDARD_KEY, CacheHeaders.NONE)
    assertNotNull(record)
    assertEquals(expected = STANDARD_KEY, actual = record.key)
  }

  @Test
  fun testMultipleRecordSelection() {
    createRecord(STANDARD_KEY)
    createRecord(QUERY_ROOT_KEY)
    val selectionKeys = setOf(STANDARD_KEY, QUERY_ROOT_KEY)
    val records = cache.loadRecords(selectionKeys, CacheHeaders.NONE)
    val selectedKeys = records.map { it.key }.toSet()
    assertEquals(selectionKeys, selectedKeys)
  }

  @Test
  fun testRecordSelection_root() {
    createRecord(QUERY_ROOT_KEY)
    val record = requireNotNull(cache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE))
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
    cache.merge(
        record = Record(
            key = STANDARD_KEY,
            fields = mapOf(
                "fieldKey" to "valueUpdated",
                "newFieldKey" to true,
            ),
        ),
        cacheHeaders = CacheHeaders.NONE,
    )
    val record = cache.loadRecord(STANDARD_KEY, CacheHeaders.NONE)
    assertNotNull(record)
    assertEquals(expected = "valueUpdated", actual = record.fields["fieldKey"])
    assertEquals(expected = true, actual = record.fields["newFieldKey"])
  }

  @Test
  fun testRecordDelete() {
    createRecord(STANDARD_KEY)
    cache.merge(
        record = Record(
            key = STANDARD_KEY,
            fields = mapOf(
                "fieldKey" to "valueUpdated",
                "newFieldKey" to true,
            ),
        ),
        cacheHeaders = CacheHeaders.NONE,
    )
    cache.remove(cacheKey = CacheKey(STANDARD_KEY), cascade = false)
    val record = cache.loadRecord(STANDARD_KEY, CacheHeaders.NONE)
    assertNull(record)
  }

  @Test
  fun testClearAll() {
    createRecord(QUERY_ROOT_KEY)
    createRecord(STANDARD_KEY)
    cache.clearAll()
    assertNull(cache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE))
    assertNull(cache.loadRecord(STANDARD_KEY, CacheHeaders.NONE))
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
  fun testHeader_evictAfterBatchRead() {
    createRecord(STANDARD_KEY)
    createRecord(QUERY_ROOT_KEY)
    val selectionSet = setOf(STANDARD_KEY, QUERY_ROOT_KEY)
    val records = cache.loadRecords(selectionSet, CacheHeaders.builder()
        .addHeader(ApolloCacheHeaders.EVICT_AFTER_READ, "true").build())
    assertEquals(records.size, 2)
    val emptyRecords = cache.loadRecords(selectionSet, CacheHeaders.builder()
        .addHeader(ApolloCacheHeaders.EVICT_AFTER_READ, "true").build())
    assertTrue(emptyRecords.isEmpty())
  }

  @Test
  fun testHeader_noCache() {
    cache.merge(
        record = Record(
            key = STANDARD_KEY,
            fields = emptyMap(),
        ),
        cacheHeaders = CacheHeaders.builder().addHeader(ApolloCacheHeaders.DO_NOT_STORE, "true").build(),
    )
    val record = cache.loadRecord(STANDARD_KEY, CacheHeaders.NONE)
    assertNull(record)
  }

  @Test
  fun testRecordMerge_noOldRecord() {
    val changedKeys = cache.merge(
        record = Record(
            key = STANDARD_KEY,
            fields = mapOf(
                "fieldKey" to "valueUpdated",
                "newFieldKey" to true,
            ),
        ),
        cacheHeaders = CacheHeaders.NONE,
    )
    val record = cache.loadRecord(STANDARD_KEY, CacheHeaders.NONE)
    assertNotNull(record)
    assertEquals(expected = setOf("$STANDARD_KEY.fieldKey", "$STANDARD_KEY.newFieldKey"), actual = changedKeys)
    assertEquals(expected = "valueUpdated", actual = record.fields["fieldKey"])
    assertEquals(expected = true, actual = record.fields["newFieldKey"])
  }

  @Test
  fun testRecordMerge_withOldRecord() {
    createRecord(STANDARD_KEY)
    cache.merge(
        record = Record(
            key = STANDARD_KEY,
            fields = mapOf(
                "fieldKey" to "valueUpdated",
                "newFieldKey" to true,
            ),
        ),
        cacheHeaders = CacheHeaders.NONE
    )
    val record = cache.loadRecord(STANDARD_KEY, CacheHeaders.NONE)
    assertNotNull(record)
    assertEquals(expected = "valueUpdated", actual = record.fields["fieldKey"])
    assertEquals(expected = true, actual = record.fields["newFieldKey"])
  }

  @Test
  fun testPatternRemove() {
    createRecord("specialKey1")
    createRecord("specialKey2")
    createRecord("regularKey1")

    cache.remove("specialKey%")
    assertNull(cache.loadRecord("specialKey1", CacheHeaders.NONE))
    assertNull(cache.loadRecord("specialKey1", CacheHeaders.NONE))
    assertNotNull(cache.loadRecord("regularKey1", CacheHeaders.NONE))
  }

  @Test
  fun testPatternRemoveWithEscape() {
    createRecord("%1")

    cache.remove("\\%%")
    assertNull(cache.loadRecord("%1", CacheHeaders.NONE))
  }


  private fun createRecord(key: String) {
    cache.merge(
        record = Record(
            key = key,
            fields = mapOf(
                "field1" to "value1",
                "field2" to "value2",
            ),
        ),
        cacheHeaders = CacheHeaders.NONE,
    )
  }

  companion object {
    const val STANDARD_KEY = "key"
    const val QUERY_ROOT_KEY = "QUERY_ROOT"
  }
}
