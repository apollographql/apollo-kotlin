package com.apollographql.android.cache.normalized.lru;

import com.apollographql.android.cache.normalized.CacheStore;
import com.apollographql.android.cache.normalized.Record;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;

public class LruCacheStoreTest {

  @Test
  public void testEvictionPolicyBuilder() {
    final EvictionPolicy policy = EvictionPolicy.builder()
        .maxSizeBytes(100)
        .maxEntries(50)
        .expireAfterAccess(5, TimeUnit.HOURS)
        .expireAfterWrite(10, TimeUnit.DAYS)
        .build();

    assertThat(policy.maxSizeBytes().get()).isEqualTo(100);

    assertThat(policy.maxEntries().get()).isEqualTo(50);

    assertThat(policy.expireAfterAccess().get()).isEqualTo(5);
    assertThat(policy.expireAfterAccessTimeUnit().get()).isEqualTo(TimeUnit.HOURS);

    assertThat(policy.expireAfterWrite().get()).isEqualTo(10);
    assertThat(policy.expireAfterWriteTimeUnit().get()).isEqualTo(TimeUnit.DAYS);
  }

  @Test
  public void testSaveAndLoad_singleRecord() {
    LruCacheStore lruCacheStore = new LruCacheStore(EvictionPolicy.builder().maxSizeBytes(10 * 1024).build());
    Record testRecord = createTestRecord("1");

    lruCacheStore.merge(testRecord);

    assertTestRecordPresentAndAccurate(testRecord, lruCacheStore);
  }

  @Test
  public void testSaveAndLoad_multipleRecord_readSingle() {
    LruCacheStore lruCacheStore = new LruCacheStore(EvictionPolicy.builder().maxSizeBytes(10 * 1024).build());
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
    LruCacheStore lruCacheStore = new LruCacheStore(EvictionPolicy.builder().maxSizeBytes(10 * 1024).build());
    Record testRecord1 = createTestRecord("1");
    Record testRecord2 = createTestRecord("2");
    Record testRecord3 = createTestRecord("3");

    List<Record> inputRecords = Arrays.asList(testRecord1, testRecord2, testRecord3);
    lruCacheStore.merge(inputRecords);
    final Collection<Record> readRecords = lruCacheStore.loadRecords(Arrays.asList("key1", "key2", "key3"));
    //noinspection ResultOfMethodCallIgnored
    assertThat(readRecords).containsExactlyElementsIn(inputRecords);
  }

  @Test
  public void testEviction() {
    LruCacheStore lruCacheStore = new LruCacheStore(EvictionPolicy.builder().maxSizeBytes(2000).build());

    Record.Builder testRecord1Builder = Record.builder("key1");
    testRecord1Builder.addField("a",  new String(new byte[1100]));
    Record testRecord1 = testRecord1Builder.build();

    Record.Builder testRecord2Builder = Record.builder("key2");
    testRecord2Builder.addField("a",  new String(new byte[1100]));
    Record testRecord2 = testRecord2Builder.build();

    Record.Builder testRecord3Builder = Record.builder("key3");
    testRecord3Builder.addField("a",  new String(new byte[10]));
    Record testRecord3 = testRecord3Builder.build();

    List<Record> records = Arrays.asList(
        testRecord1,
        testRecord2,
        testRecord3
    );
    lruCacheStore.merge(records);

    //Cache does not reveal exactly how it handles eviction, but appears
    //to evict more than is strictly necessary. Regardless, any sane eviction
    //strategy should leave the third record in this test case, and evict the first record.
    assertThat(lruCacheStore.loadRecord("key1")).isNull();
    assertThat(lruCacheStore.loadRecord("key3")).isNotNull();

  }

  @Test
  public void testEviction_recordChange() {
    LruCacheStore lruCacheStore = new LruCacheStore(EvictionPolicy.builder().maxSizeBytes(2000).build());

    Record.Builder testRecord1Builder = Record.builder("key1");
    testRecord1Builder.addField("a",  new String(new byte[10]));
    Record testRecord1 = testRecord1Builder.build();

    Record.Builder testRecord2Builder = Record.builder("key2");
    testRecord2Builder.addField("a",  new String(new byte[10]));
    Record testRecord2 = testRecord2Builder.build();

    Record.Builder testRecord3Builder = Record.builder("key3");
    testRecord3Builder.addField("a",  new String(new byte[10]));
    Record testRecord3 = testRecord3Builder.build();

    List<Record> records = Arrays.asList(
        testRecord1,
        testRecord2,
        testRecord3
    );
    lruCacheStore.merge(records);

    //All records should present
    assertThat(lruCacheStore.loadRecord("key1")).isNotNull();
    assertThat(lruCacheStore.loadRecord("key2")).isNotNull();
    assertThat(lruCacheStore.loadRecord("key3")).isNotNull();

    Record.Builder largeTestRecordBuilder = Record.builder("key1");
    largeTestRecordBuilder.addField("a",  new String(new byte[2000]));
    Record largeTestRecord = largeTestRecordBuilder.build();

    lruCacheStore.merge(largeTestRecord);
    //The large record (Record 1) should be evicted. the other small records should remain.
    assertThat(lruCacheStore.loadRecord("key1")).isNull();
    assertThat(lruCacheStore.loadRecord("key2")).isNotNull();
    assertThat(lruCacheStore.loadRecord("key3")).isNotNull();

  }

  @Test
  public void testDualCacheSingleRecord() {
    LruCacheStore secondaryCacheStore = new LruCacheStore(EvictionPolicy.NO_EVICTION);
    LruCacheStore primaryCacheStore = new LruCacheStore(EvictionPolicy.NO_EVICTION, secondaryCacheStore);

    Record.Builder recordBuilder = Record.builder("root");
    recordBuilder.addField("bar", "bar");
    final Record record = recordBuilder.build();
    primaryCacheStore.merge(record);

    //verify write through behavior
    assertThat(primaryCacheStore.loadRecord("root").field("bar")).isEqualTo("bar");
    assertThat(secondaryCacheStore.loadRecord("root").field("bar")).isEqualTo("bar");
  }

  @Test
  public void testDualCacheMultipleRecord() {
    LruCacheStore secondaryCacheStore = new LruCacheStore(EvictionPolicy.NO_EVICTION);
    LruCacheStore primaryCacheStore = new LruCacheStore(EvictionPolicy.NO_EVICTION, secondaryCacheStore);

    Record.Builder recordBuilder = Record.builder("root1");
    recordBuilder.addField("bar", "bar");
    final Record record1 = recordBuilder.build();

    recordBuilder = Record.builder("root2");
    recordBuilder.addField("bar", "bar");
    final Record record2 = recordBuilder.build();

    recordBuilder = Record.builder("root3");
    recordBuilder.addField("bar", "bar");
    final Record record3 = recordBuilder.build();

    Collection<Record> records = Arrays.asList(record1, record2, record3);
    Collection<String> keys = Arrays.asList(record1.key(), record2.key(), record3.key());

    primaryCacheStore.merge(records);

    assertThat(primaryCacheStore.loadRecords(keys).size()).isEqualTo(3);

    //verify write through behavior
    assertThat(primaryCacheStore.loadRecords(keys).size()).isEqualTo(3);
    assertThat(secondaryCacheStore.loadRecords(keys).size()).isEqualTo(3);
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
