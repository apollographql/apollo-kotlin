package com.apollographql.android.cache.normalized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

public abstract class CacheStore {

  @Nullable public abstract Record loadRecord(String key);

  public Collection<Record> loadRecords(Collection<String> keys) {
    List<Record> records = new ArrayList<>(keys.size());
    for (String key : keys) {
      final Record record = loadRecord(key);
      if (record != null) {
        records.add(record);
      }
    }
    return records;
  }

  public abstract void merge(Record object);

  public void merge(Collection<Record> recordSet) {
    for (Record record : recordSet) {
      merge(record);
    }
  }

}
