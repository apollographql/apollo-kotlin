package com.apollographql.android.cache.normalized;

import java.util.Collection;

public final class InMemoryCacheStore extends CacheStore {

  private final RecordSet recordSet;

  public InMemoryCacheStore() {
    this.recordSet = new RecordSet();
  }

  @Override public Record loadRecord(String key) {
    return recordSet.get(key);
  }

  @Override public synchronized void merge(Record apolloRecord) {
    recordSet.merge(apolloRecord);
  }

  public Collection<Record> allRecords() {
    return recordSet.allRecords();
  }
}
