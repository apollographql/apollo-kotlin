package com.apollographql.android.impl;

import com.apollographql.android.cache.normalized.CacheStore;
import com.apollographql.android.cache.normalized.Record;
import com.apollographql.android.cache.normalized.lru.LruCacheStore;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class LruCacheStoreTest {

  @Test
  public void testSaveAndLoad_singleRecord() {
    LruCacheStore lruCacheStore = LruCacheStore.createWithMaximumByteSize(10 * 1024);
    Record testRecord = createTestRecord("1");

    lruCacheStore.merge(testRecord);

    assertTestRecordPresentAndAccurate(testRecord, lruCacheStore);
  }

  @Test
  public void testSaveAndLoad_multipleRecord_readSingle() {
    LruCacheStore lruCacheStore = LruCacheStore.createWithMaximumByteSize(10 * 1024);
    Record testRecord1 = createTestRecord("1");
    Record testRecord2 = createTestRecord("2");
    Record testRecord3 = createTestRecord("3");

    List<Record> records = Arrays.asList(testRecord1, testRecord2, testRecord3);
    lruCacheStore.merge(records);

    assertTestRecordPresentAndAccurate(testRecord1, lruCacheStore);
    assertTestRecordPresentAndAccurate(testRecord2, lruCacheStore);
    assertTestRecordPresentAndAccurate(testRecord3, lruCacheStore);
  }

  @Test
  public void testSaveAndLoad_multipleRecord_readMultiple() {
    LruCacheStore lruCacheStore = LruCacheStore.createWithMaximumByteSize(10 * 1024);
    Record testRecord1 = createTestRecord("1");
    Record testRecord2 = createTestRecord("2");
    Record testRecord3 = createTestRecord("3");

    List<Record> inputRecords = Arrays.asList(testRecord1, testRecord2, testRecord3);
    lruCacheStore.merge(inputRecords);
    final Collection<Record> readRecords = lruCacheStore.loadRecords(Arrays.asList("1", "2", "3"));
    //noinspection ResultOfMethodCallIgnored
    assertThat(readRecords).containsExactlyElementsIn(inputRecords);
  }

  @Test
  public void testEviction() {
    LruCacheStore lruCacheStore = LruCacheStore.createWithMaximumByteSize(2000);

    Record.Builder testRecord1 = Record.builder("key1");
    testRecord1.addField("a",  new String(new byte[1100]));

    Record.Builder testRecord2 = Record.builder("key2");
    testRecord2.addField("a",  new String(new byte[1100]));

    Record.Builder testRecord3 = Record.builder("key3");
    testRecord3.addField("a",  new String(new byte[10]));

    List<Record> records = Arrays.asList(
        testRecord1.build(),
        testRecord2.build(),
        testRecord3.build()
    );
    lruCacheStore.merge(records);

    //Cache does not reveal exactly how it handles eviction, but appears
    //to evict more than is strictly necessary. Regardless, any sane eviction
    //strategy should leave the third record in this test case, and evict the first record.
    assertThat(lruCacheStore.loadRecord("key1")).isNull();
    assertThat(lruCacheStore.loadRecord("key3")).isNotNull();

  }

  private void assertTestRecordPresentAndAccurate(Record testRecord, CacheStore store) {
    final Record cacheRecord1 = store.loadRecord(testRecord.key());
    assertThat(cacheRecord1.key()).isEqualTo(testRecord.key());
    assertThat(cacheRecord1.field("a")).isEqualTo(testRecord.field("a"));
    assertThat(cacheRecord1.field("b")).isEqualTo(testRecord.field("b"));
  }

  private Record createTestRecord(String id) {
    Record.Builder testRecord = Record.builder("key" + id);
    testRecord.addField("a", "stringValueA" + id);
    testRecord.addField("b", "stringValueB" + id);
    return testRecord.build();
  }
}
