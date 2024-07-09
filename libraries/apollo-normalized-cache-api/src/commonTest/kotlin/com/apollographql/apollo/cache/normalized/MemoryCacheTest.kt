package com.apollographql.apollo.cache.normalized

import com.apollographql.apollo.cache.normalized.api.ApolloCacheHeaders
import com.apollographql.apollo.cache.normalized.api.CacheHeaders
import com.apollographql.apollo.cache.normalized.api.CacheKey
import com.apollographql.apollo.cache.normalized.api.MemoryCache
import com.apollographql.apollo.cache.normalized.api.NormalizedCache
import com.apollographql.apollo.cache.normalized.api.Record
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MemoryCacheTest {
  @Test
  fun testSaveAndLoad_singleRecord() {
    val lruCache = createCache()
    val testRecord = createTestRecord("1")
    lruCache.merge(testRecord, CacheHeaders.NONE)

    assertTestRecordPresentAndAccurate(testRecord, lruCache)
  }

  @Test
  fun testSaveAndLoad_multipleRecord_readSingle() {
    val lruCache = createCache()
    val testRecord1 = createTestRecord("1")
    val testRecord2 = createTestRecord("2")
    val testRecord3 = createTestRecord("3")
    val records = listOf(testRecord1, testRecord2, testRecord3)
    lruCache.merge(records, CacheHeaders.NONE)

    assertTestRecordPresentAndAccurate(testRecord1, lruCache)
    assertTestRecordPresentAndAccurate(testRecord2, lruCache)
    assertTestRecordPresentAndAccurate(testRecord3, lruCache)
  }

  @Test
  fun testSaveAndLoad_multipleRecord_readMultiple() {
    val lruCache = createCache()
    val testRecord1 = createTestRecord("1")
    val testRecord2 = createTestRecord("2")
    val testRecord3 = createTestRecord("3")
    val records = listOf(testRecord1, testRecord2, testRecord3)
    lruCache.merge(records, CacheHeaders.NONE)

    val readRecords = lruCache.loadRecords(listOf("key1", "key2", "key3"), CacheHeaders.NONE)
    assertTrue(readRecords.containsAll(records))
  }

  @Test
  fun testLoad_recordNotPresent() {
    val lruCache = createCache()
    val record = lruCache.loadRecord("key1", CacheHeaders.NONE)
    assertNull(record)
  }

  @Test
  fun testEviction() {
    val testRecord1 = createTestRecord("1")
    val testRecord2 = createTestRecord("2")
    val testRecord3 = createTestRecord("3")

    val lruCache = createCache(
        // all records won't fit as there is timestamp that stored with each record
        maxSizeBytes = 200
    )

    val records = listOf(testRecord1, testRecord2, testRecord3)
    lruCache.merge(records, CacheHeaders.NONE)

    //Cache does not reveal exactly how it handles eviction, but appears
    //to evict more than is strictly necessary. Regardless, any sane eviction
    //strategy should leave the third record in this test case, and evict the first record.
    assertNull(lruCache.loadRecord(testRecord1.key, CacheHeaders.NONE))
    assertNotNull(lruCache.loadRecord(testRecord3.key, CacheHeaders.NONE))
  }

  @Test
  fun testEviction_recordChange() {
    val testRecord1 = createTestRecord("1")
    val testRecord2 = createTestRecord("2")
    val testRecord3 = createTestRecord("3")

    val lruCache = createCache(
        maxSizeBytes = 240
    )

    val records = listOf(testRecord1, testRecord2, testRecord3)
    lruCache.merge(records, CacheHeaders.NONE)

    assertNotNull(lruCache.loadRecord(testRecord1.key, CacheHeaders.NONE))
    assertNotNull(lruCache.loadRecord(testRecord2.key, CacheHeaders.NONE))
    assertNotNull(lruCache.loadRecord(testRecord3.key, CacheHeaders.NONE))

    val updatedRestRecord1 = Record(
        fields = testRecord1.fields.plus("field3" to "value3"),
        key = testRecord1.key,
        mutationId = testRecord1.mutationId
    )

    lruCache.merge(updatedRestRecord1, CacheHeaders.NONE)

    assertNotNull(lruCache.loadRecord(testRecord1.key, CacheHeaders.NONE))
    assertNotNull(lruCache.loadRecord(testRecord2.key, CacheHeaders.NONE))
    assertNotNull(lruCache.loadRecord(testRecord3.key, CacheHeaders.NONE))
  }

  @Test
  fun testExpiresImmediatly() {
    val testRecord = createTestRecord("")
    val lruCache = createCache(expireAfterMillis = 0)
    lruCache.merge(testRecord, CacheHeaders.NONE)

    assertNull(lruCache.loadRecord(testRecord.key, CacheHeaders.NONE))
  }


  @Test
  fun testDualCacheSingleRecord() {
    val secondaryCache = createCache()
    val primaryCache = createCache().chain(secondaryCache)

    val mockRecord = createTestRecord("")
    primaryCache.merge(mockRecord, CacheHeaders.NONE)

    //verify write through behavior
    assertEquals(mockRecord.fields, primaryCache.loadRecord(mockRecord.key, CacheHeaders.NONE)?.fields)
    assertEquals(mockRecord.fields, secondaryCache.loadRecord(mockRecord.key, CacheHeaders.NONE)?.fields)
  }

  @Test
  fun testDualCacheMultipleRecord() {
    val secondaryCache = createCache()
    val primaryCache = createCache().chain(secondaryCache)

    val testRecord1 = createTestRecord("1")
    val testRecord2 = createTestRecord("2")
    val testRecord3 = createTestRecord("3")
    val records = listOf(testRecord1, testRecord2, testRecord3)
    primaryCache.merge(records, CacheHeaders.NONE)

    val keys = listOf(testRecord1.key, testRecord2.key, testRecord3.key)
    assertEquals(3, primaryCache.loadRecords(keys, CacheHeaders.NONE).size)
    assertEquals(3, secondaryCache.loadRecords(keys, CacheHeaders.NONE).size)
  }

  @Test
  fun testDualCache_recordNotPresent() {
    val secondaryCache = createCache()
    val primaryCache = createCache().chain(secondaryCache)
    assertNull(primaryCache.loadRecord("key", CacheHeaders.NONE))
  }


  @Test
  fun testDualCache_clearAll() {
    val secondaryCache = createCache()
    val primaryCache = createCache().chain(secondaryCache) as MemoryCache

    val testRecord1 = createTestRecord("1")
    val testRecord2 = createTestRecord("2")
    val testRecord3 = createTestRecord("3")
    val records = listOf(testRecord1, testRecord2, testRecord3)
    primaryCache.merge(records, CacheHeaders.NONE)

    primaryCache.clearAll()

    assertEquals(0, primaryCache.size)
    assertEquals(0, secondaryCache.size)
  }

  @Test
  fun testDualCache_readFromNext() {
    val secondaryCache = createCache()
    val primaryCache = createCache().chain(secondaryCache) as MemoryCache

    val testRecord = createTestRecord("")
    primaryCache.merge(testRecord, CacheHeaders.NONE)

    primaryCache.clearCurrentCache()

    assertEquals(testRecord.fields, primaryCache.loadRecord(testRecord.key, CacheHeaders.NONE)?.fields)
  }


  // Tests for StandardCacheHeader compliance.
  @Test
  fun testHeader_evictAfterRead() {
    val lruCache = createCache()
    val testRecord = createTestRecord("1")

    lruCache.merge(testRecord, CacheHeaders.NONE)

    val headers = CacheHeaders.builder().addHeader(ApolloCacheHeaders.EVICT_AFTER_READ, "true").build()

    assertNotNull(lruCache.loadRecord(testRecord.key, headers))
    assertNull(lruCache.loadRecord(testRecord.key, headers))
  }

  @Test
  fun testHeader_noCache() {
    val lruCache = createCache()
    val testRecord = createTestRecord("1")

    val headers = CacheHeaders.builder().addHeader(ApolloCacheHeaders.DO_NOT_STORE, "true").build()

    lruCache.merge(testRecord, headers)

    assertNull(lruCache.loadRecord(testRecord.key, headers))
  }

  @Test
  fun testDump() {
    val lruCache = createCache()

    val testRecord1 = createTestRecord("1")
    val testRecord2 = createTestRecord("2")
    val testRecord3 = createTestRecord("3")
    val records = listOf(testRecord1, testRecord2, testRecord3)
    lruCache.merge(records, CacheHeaders.NONE)

    with(lruCache.dump()) {
      val cache = this[MemoryCache::class]!!

      assertTrue(cache.keys.containsAll(records.map { it.key }))
      assertEquals(testRecord1, cache[testRecord1.key])
      assertEquals(testRecord2, cache[testRecord2.key])
      assertEquals(testRecord3, cache[testRecord3.key])
    }
  }


  @Test
  fun testRemove_cascadeFalse() {
    val lruCache = createCache()

    val record1 = Record(
        key = "id_1",
        fields = mapOf(
            "a" to "stringValueA",
            "b" to "stringValueB"
        )
    )

    val record2 = Record(
        key = "id_2",
        fields = mapOf(
            "a" to CacheKey("id_1"),
        )
    )

    val records = listOf(record1, record2)
    lruCache.merge(records, CacheHeaders.NONE)

    assertTrue(lruCache.remove(CacheKey(record2.key), cascade = false))
    assertNotNull(lruCache.loadRecord(record1.key, CacheHeaders.NONE))
  }

  @Test
  fun testRemove_cascadeTrue() {
    val lruCache = createCache()

    val record1 = Record(
        key = "id_1",
        fields = mapOf(
            "a" to "stringValueA",
            "b" to "stringValueB"
        )
    )

    val record2 = Record(
        key = "id_2",
        fields = mapOf(
            "a" to CacheKey("id_1"),
        )
    )

    val records = listOf(record1, record2)
    lruCache.merge(records, CacheHeaders.NONE)

    assertTrue(lruCache.remove(CacheKey(record2.key), cascade = true))
    assertNull(lruCache.loadRecord(record1.key, CacheHeaders.NONE))
  }

  private fun createCache(
      maxSizeBytes: Int = 10 * 1024,
      expireAfterMillis: Long = -1,
  ): MemoryCache {
    return MemoryCache(maxSizeBytes = maxSizeBytes, expireAfterMillis = expireAfterMillis)
  }

  private fun assertTestRecordPresentAndAccurate(testRecord: Record, store: NormalizedCache) {
    val cacheRecord = checkNotNull(store.loadRecord(testRecord.key, CacheHeaders.NONE))
    assertEquals(testRecord.key, cacheRecord.key)
    assertEquals(testRecord.fields, cacheRecord.fields)
  }

  private fun createTestRecord(id: String): Record {
    return Record(
        key = "key$id",
        fields = mapOf(
            "field1" to "stringValueA$id",
            "field2" to "stringValueB$id"
        )
    )
  }
}
