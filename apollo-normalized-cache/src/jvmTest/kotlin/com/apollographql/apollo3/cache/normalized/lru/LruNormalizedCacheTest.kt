package com.apollographql.apollo3.cache.normalized.lru

import com.apollographql.apollo3.cache.ApolloCacheHeaders
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.CacheHeaders.Companion.builder
import com.apollographql.apollo3.cache.normalized.NormalizedCache
import com.apollographql.apollo3.cache.normalized.Record
import com.apollographql.apollo3.cache.normalized.RecordFieldJsonAdapter
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.CacheReference

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.concurrent.TimeUnit

class LruNormalizedCacheTest {

  @Test
  fun testEvictionPolicyBuilder() {
    val policy = EvictionPolicy.builder()
        .maxSizeBytes(100)
        .maxEntries(50)
        .expireAfterAccess(5, TimeUnit.HOURS)
        .expireAfterWrite(10, TimeUnit.DAYS)
        .build()

    with(policy) {
      assertThat(maxSizeBytes).isEqualTo(100)
      assertThat(maxEntries).isEqualTo(50)
      assertThat(expireAfterAccess).isEqualTo(5)
      assertThat(expireAfterAccessTimeUnit).isEqualTo(TimeUnit.HOURS)
      assertThat(expireAfterWrite).isEqualTo(10)
      assertThat(expireAfterWriteTimeUnit).isEqualTo(TimeUnit.DAYS)
    }
  }

  @Test
  fun testSaveAndLoad_singleRecord() {
    val lruCache = createLruNormalizedCache()
    val testRecord = createTestRecord("1")
    lruCache.merge(testRecord, CacheHeaders.NONE)

    assertTestRecordPresentAndAccurate(testRecord, lruCache)
  }

  @Test
  fun testSaveAndLoad_multipleRecord_readSingle() {
    val lruCache = createLruNormalizedCache()
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
    val lruCache = createLruNormalizedCache()
    val testRecord1 = createTestRecord("1")
    val testRecord2 = createTestRecord("2")
    val testRecord3 = createTestRecord("3")
    val inputRecords = listOf(testRecord1, testRecord2, testRecord3)
    lruCache.merge(inputRecords, CacheHeaders.NONE)

    val readRecords = lruCache.loadRecords(listOf("key1", "key2", "key3"), CacheHeaders.NONE)
    assertThat(readRecords).containsExactlyElementsIn(inputRecords)
  }

  @Test
  fun testLoad_recordNotPresent() {
    val lruCache = createLruNormalizedCache()
    val record = lruCache.loadRecord("key1", CacheHeaders.NONE)
    assertThat(record).isNull()
  }

  @Test
  fun testEviction() {
    val lruCache = LruNormalizedCacheFactory(EvictionPolicy.builder().maxSizeBytes(2000).build()).create()
    val testRecord1 = Record(
        key = "key1",
        fields = mapOf(
            "a" to String(ByteArray(1100)),
        ),
    )
    val testRecord2 = Record(
        key = "key2",
        fields = mapOf(
            "a" to String(ByteArray(1100)),
        ),
    )
    val testRecord3 = Record(
        key = "key3",
        fields = mapOf(
            "a" to String(ByteArray(10)),
        ),
    )
    val records = listOf(testRecord1, testRecord2, testRecord3)
    lruCache.merge(records, CacheHeaders.NONE)

    //Cache does not reveal exactly how it handles eviction, but appears
    //to evict more than is strictly necessary. Regardless, any sane eviction
    //strategy should leave the third record in this test case, and evict the first record.
    assertThat(lruCache.loadRecord("key1", CacheHeaders.NONE)).isNull()
    assertThat(lruCache.loadRecord("key3", CacheHeaders.NONE)).isNotNull()
  }

  @Test
  fun testEviction_recordChange() {
    val lruCache = LruNormalizedCacheFactory(EvictionPolicy.builder().maxSizeBytes(2000).build()).create()
    val testRecord1 = Record(
        key = "key1",
        fields = mapOf(
            "a" to String(ByteArray(10)),
        ),
    )
    val testRecord2 = Record(
        key = "key2",
        fields = mapOf(
            "a" to String(ByteArray(10)),
        ),
    )
    val testRecord3 = Record(
        key = "key3",
        fields = mapOf(
            "a" to String(ByteArray(10)),
        ),
    )
    val records = listOf(testRecord1, testRecord2, testRecord3)
    lruCache.merge(records, CacheHeaders.NONE)

    //All records should present
    assertThat(lruCache.loadRecord("key1", CacheHeaders.NONE)).isNotNull()
    assertThat(lruCache.loadRecord("key2", CacheHeaders.NONE)).isNotNull()
    assertThat(lruCache.loadRecord("key3", CacheHeaders.NONE)).isNotNull()

    val largeTestRecord = Record(
        key = "key1",
        fields = mapOf(
            "a" to String(ByteArray(2000)),
        ),
    )
    lruCache.merge(largeTestRecord, CacheHeaders.NONE)
    //The large record (Record 1) should be evicted. the other small records should remain.
    assertThat(lruCache.loadRecord("key1", CacheHeaders.NONE)).isNull()
    assertThat(lruCache.loadRecord("key2", CacheHeaders.NONE)).isNotNull()
    assertThat(lruCache.loadRecord("key3", CacheHeaders.NONE)).isNotNull()
  }

  @Test
  fun testDualCacheSingleRecord() {
    val secondaryCacheFactory = LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION)
    val primaryCache = LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION)
        .chain(secondaryCacheFactory).createChain()
    val record = Record(
        key = "root",
        fields = mapOf(
            "bar" to "bar",
        ),
    )
    primaryCache.merge(record, CacheHeaders.NONE)

    //verify write through behavior
    assertThat(primaryCache.loadRecord("root", CacheHeaders.NONE)!!.get("bar")).isEqualTo("bar")
    assertThat(primaryCache.nextCache!!.loadRecord("root", CacheHeaders.NONE)!!.get("bar")).isEqualTo("bar")
  }

  @Test
  fun testDualCacheMultipleRecord() {
    val secondaryCacheFactory = LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION)
    val primaryCache = LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION)
        .chain(secondaryCacheFactory).createChain()

    val record1 = Record(
        key = "root1",
        fields = mapOf(
            "bar" to "bar",
        ),
    )
    val record2 = Record(
        key = "root2",
        fields = mapOf(
            "bar" to "bar",
        )
    )
    val record3 = Record(
        key = "root3",
        fields = mapOf(
            "bar" to "bar",
        )
    )

    val records = listOf(record1, record2, record3)
    val keys = listOf(record1.key, record2.key, record3.key)
    primaryCache.merge(records, CacheHeaders.NONE)
    assertThat(primaryCache.loadRecords(keys, CacheHeaders.NONE)).hasSize(3)

    //verify write through behavior
    assertThat(primaryCache.loadRecords(keys, CacheHeaders.NONE)).hasSize(3)
    assertThat(primaryCache.nextCache!!.loadRecords(keys, CacheHeaders.NONE)).hasSize(3)
  }

  @Test
  fun testDualCache_recordNotPresent() {
    val secondaryCacheFactory = LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION)
    val primaryCacheStore = LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION)
        .chain(secondaryCacheFactory).createChain()

    assertThat(primaryCacheStore.loadRecord("not_present_id", CacheHeaders.NONE)).isNull()
  }

  @Test
  fun testClearAll() {
    val secondaryCacheFactory = LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION)
    val primaryCacheStore = LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION)
        .chain(secondaryCacheFactory).createChain()
    val record = Record(key = "key", fields = emptyMap())
    primaryCacheStore.merge(record, CacheHeaders.NONE)
    primaryCacheStore.clearAll()

    assertThat(primaryCacheStore.loadRecord("key", CacheHeaders.NONE)).isNull()
  }

  @Test
  fun testClearSecondaryCache() {
    val secondaryCacheFactory = LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION)
    val primaryCache = LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION)
        .chain(secondaryCacheFactory).createChain()
    val record = Record(key = "key", fields = emptyMap())

    val nextCache = requireNotNull(primaryCache.nextCache)
    primaryCache.merge(record, CacheHeaders.NONE)
    nextCache.clearAll()

    assertThat(nextCache.loadRecord("key", CacheHeaders.NONE)).isNull()
  }

  // Tests for StandardCacheHeader compliance.
  @Test
  fun testHeader_evictAfterRead() {
    val lruCache = createLruNormalizedCache()
    val testRecord = createTestRecord("1")
    lruCache.merge(testRecord, CacheHeaders.NONE)
    val record = lruCache.loadRecord("key1", builder().addHeader(ApolloCacheHeaders.EVICT_AFTER_READ, "true")
        .build())
    assertThat(record).isNotNull()
    val nullRecord = lruCache.loadRecord("key1", builder().addHeader(ApolloCacheHeaders.EVICT_AFTER_READ, "true")
        .build())
    assertThat(nullRecord).isNull()
  }

  @Test
  fun testHeader_noCache() {
    val lruCache = createLruNormalizedCache()
    val testRecord = createTestRecord("1")
    lruCache.merge(testRecord, builder().addHeader(ApolloCacheHeaders.DO_NOT_STORE, "true").build())
    val record = lruCache.loadRecord("key1", CacheHeaders.NONE)
    assertThat(record).isNull()
    val testRecord1 = createTestRecord("1")
    val testRecord2 = createTestRecord("2")
    val testRecordSet: MutableCollection<Record> = HashSet()
    testRecordSet.add(testRecord1)
    testRecordSet.add(testRecord2)
    lruCache.merge(testRecordSet, builder().addHeader(ApolloCacheHeaders.DO_NOT_STORE, "true").build())
    val record1 = lruCache.loadRecord("key1", CacheHeaders.NONE)
    val record2 = lruCache.loadRecord("key2", CacheHeaders.NONE)
    assertThat(record1).isNull()
    assertThat(record2).isNull()
  }

  @Test
  fun testSettingMaxSizeAndMaxEntries() {
    val policy = EvictionPolicy.builder()
        .maxSizeBytes(100)
        .maxEntries(50)
        .build()

    val exception: Throwable? = try {
      createLruNormalizedCache(policy)
      null
    } catch (e: Throwable) {
      e
    }

    assertThat(exception!!).let {
      it.isInstanceOf(IllegalStateException::class.java)
      it.hasMessage("maximum weight was already set to 100")
    }
  }

  @Test
  fun testRemove_nonExistentRecord() {
    val lruCache = createLruNormalizedCache()
    assertThat(lruCache.remove(CacheKey("fake"), true)).isFalse()
  }

  @Test
  fun testRemove_nonReferencedRecord() {
    val lruCache = createLruNormalizedCache()
    lruCache.merge(listOf(createTestRecord("id_1")), CacheHeaders.NONE)
    assertThat(lruCache.remove(CacheKey("keyid_1"), false)).isTrue()
  }

  @Test
  fun testDump() {
    val lruCache = createLruNormalizedCache()

    val record1 = createTestRecord("id_1")
    val record2 = createTestRecord("id_2")
    val record3 = createTestRecord("id_3")

    lruCache.merge(listOf(
        record1,
        record2,
        record3
    ), CacheHeaders.NONE)

    with(lruCache.dump()) {
      assertThat(size).isEqualTo(1)
      val cache = this[LruNormalizedCache::class]!!

      assertThat(cache.keys).containsExactly("keyid_1", "keyid_2", "keyid_3")
      assertThat(cache["keyid_1"]).isEqualTo(record1)
      assertThat(cache["keyid_2"]).isEqualTo(record2)
      assertThat(cache["keyid_3"]).isEqualTo(record3)
    }
  }

  @Test
  fun testRemove_referencedRecord_cascadeFalse() {

    val lruCache = createLruNormalizedCache()

    val record1 = Record("id_1", emptyMap())
    val record2 = Record(
        key = "id_2",
        fields = mapOf(
            "a" to CacheReference("id_1"),
        ),
    )

    lruCache.merge(
        listOf(record1, record2), CacheHeaders.NONE
    )

    assertThat(lruCache.remove(CacheKey("id_2"), cascade = false)).isTrue()
    assertThat(lruCache.loadRecord("id_1", CacheHeaders.NONE)).isNotNull()
  }

  @Test
  fun testRemove_referencedRecord_cascadeTrue() {

    val lruCache = createLruNormalizedCache()

    val record1 = Record("id_1", emptyMap())
    val record2 = Record(
        key = "id_2",
        fields = mapOf(
            "a" to CacheReference("id_1"),
        ),
    )

    lruCache.merge(
        listOf(record1, record2), CacheHeaders.NONE
    )

    assertThat(lruCache.remove(CacheKey("id_2"), cascade = true)).isTrue()
    assertThat(lruCache.loadRecord("id_1", CacheHeaders.NONE)).isNull()
  }

  private fun createLruNormalizedCache(policy: EvictionPolicy = EvictionPolicy.builder().maxSizeBytes(10 * 1024.toLong()).build()) =
      LruNormalizedCacheFactory(policy).create()

  private fun assertTestRecordPresentAndAccurate(testRecord: Record, store: NormalizedCache) {
    val cacheRecord = requireNotNull(store.loadRecord(testRecord.key, CacheHeaders.NONE))

    assertThat(cacheRecord.key).isEqualTo(testRecord.key)
    assertThat(cacheRecord.get("a")).isEqualTo(testRecord.get("a"))
    assertThat(cacheRecord.get("b")).isEqualTo(testRecord.get("b"))
  }

  private fun createTestRecord(id: String): Record {
    return Record(
        key = "key$id",
        fields = mapOf(
            "a" to "stringValueA$id",
            "b" to "stringValueB$id",
        )
    )
  }
}
