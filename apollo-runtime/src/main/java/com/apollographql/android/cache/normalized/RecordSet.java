package com.apollographql.android.cache.normalized;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RecordSet {

  private final Map<String, Record> recordMap = new LinkedHashMap<>();

  public Record get(String key) {
    return recordMap.get(key);
  }

  public void merge(Record apolloRecord) {
    final Record oldRecord = recordMap.get(apolloRecord.key());
    if (oldRecord == null) {
      recordMap.put(apolloRecord.key(), apolloRecord);
    } else {
      oldRecord.mergeWith(apolloRecord);
    }
  }

  public Collection<Record> allRecords() {
    return recordMap.values();
  }
}
