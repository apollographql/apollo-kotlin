package com.apollographql.android.cache.normalized;

import java.util.Collection;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A dual cache that is composed of a primary and secondary cache. For reads,
 * primary cache will be used if it has data. If not, secondary cache will
 * be used.
 *
 * For writes, both caches will be written to synchronously.
 */
public final class DualCacheStore extends CacheStore {

  private final CacheStore primaryCacheStore;
  private final CacheStore secondaryCacheStore;

  public DualCacheStore(CacheStore primaryCacheStore, CacheStore secondaryCacheStore) {
    this.primaryCacheStore = primaryCacheStore;
    this.secondaryCacheStore = secondaryCacheStore;
  }

  @Nullable @Override public Record loadRecord(String key) {
    Record record = primaryCacheStore.loadRecord(key);
    if (record == null) {
      record = secondaryCacheStore.loadRecord(key);
    }
    return record;
  }

  @Nonnull @Override public Collection<Record> loadRecords(Collection<String> keys) {
    Collection<Record> recordSet = primaryCacheStore.loadRecords(keys);
    if (recordSet.isEmpty()) {
      recordSet = secondaryCacheStore.loadRecords(keys);
    }
    return recordSet;
  }

  @Nonnull @Override public Set<String> merge(Collection<Record> recordSet) {
    Set<String> changedKeys;
    synchronized (this) {
      changedKeys = primaryCacheStore.merge(recordSet);
      secondaryCacheStore.merge(recordSet);
    }
    return changedKeys;
  }

  @Nonnull @Override public Set<String> merge(Record record) {
    Set<String> changedKeys;
    synchronized (this) {
      changedKeys = primaryCacheStore.merge(record);
      secondaryCacheStore.merge(record);
    }
    return changedKeys;
  }
}
