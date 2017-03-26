package com.apollographql.android.cache;

import com.apollographql.android.cache.normalized.DualCacheStore;
import com.apollographql.android.cache.normalized.Record;
import com.apollographql.android.cache.normalized.lru.EvictionPolicy;
import com.apollographql.android.cache.normalized.lru.LruCacheStore;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static com.google.common.truth.Truth.assertThat;

public class DualCacheStoreTest {

  @Test
  public void testSingleRecord() {
    LruCacheStore primaryCacheStore = new LruCacheStore(EvictionPolicy.NO_EVICTION);
    LruCacheStore secondaryCacheStore = new LruCacheStore(EvictionPolicy.NO_EVICTION);

    Record.Builder recordBuilder = Record.builder("root");
    recordBuilder.addField("bar", "bar");
    final Record record = recordBuilder.build();
    DualCacheStore dualCacheStore = new DualCacheStore(primaryCacheStore, secondaryCacheStore);
    dualCacheStore.merge(record);

    //verify write through behavior
    assertThat(dualCacheStore.loadRecord("root").field("bar")).isEqualTo("bar");
    assertThat(primaryCacheStore.loadRecord("root").field("bar")).isEqualTo("bar");
    assertThat(secondaryCacheStore.loadRecord("root").field("bar")).isEqualTo("bar");
  }

  @Test
  public void testMultipleRecord() {
    LruCacheStore primaryCacheStore = new LruCacheStore(EvictionPolicy.NO_EVICTION);
    LruCacheStore secondaryCacheStore = new LruCacheStore(EvictionPolicy.NO_EVICTION);

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


    DualCacheStore dualCacheStore = new DualCacheStore(primaryCacheStore, secondaryCacheStore);
    dualCacheStore.merge(records);

    Collection<String> keys = Arrays.asList(record1.key(), record2.key(), record3.key());

    assertThat(dualCacheStore.loadRecords(keys).size()).isEqualTo(3);

    //verify write through behavior
    assertThat(primaryCacheStore.loadRecords(keys).size()).isEqualTo(3);
    assertThat(secondaryCacheStore.loadRecords(keys).size()).isEqualTo(3);
  }
}
