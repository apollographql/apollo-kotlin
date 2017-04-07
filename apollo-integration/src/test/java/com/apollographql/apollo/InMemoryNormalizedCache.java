package com.apollographql.apollo;

import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.cache.normalized.RecordSet;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;

public final class InMemoryNormalizedCache extends NormalizedCache {

  private RecordSet recordSet;

  public InMemoryNormalizedCache() {
    this.recordSet = new RecordSet();
  }

  @Nonnull public Record loadRecord(String key) {
    return recordSet.get(key);
  }

  @Nonnull public Set<String> merge(Record apolloRecord) {
    return recordSet.merge(apolloRecord);
  }

  @Nonnull @Override public Set<String> merge(Collection<Record> recordSet) {
    Set<String> changedKeys = new LinkedHashSet<>();
    for (Record record : recordSet) {
      changedKeys.addAll(merge(record));
    }
    return changedKeys;
  }

  @Override public void clearAll() {
    recordSet = new RecordSet();
  }

  public Collection<Record> allRecords() {
    return recordSet.allRecords();
  }
}
