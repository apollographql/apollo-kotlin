package com.apollographql.apollo.cache.normalized;

import com.apollographql.apollo.cache.normalized.sql.RecordFieldAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class CacheStore {

  @Nullable public abstract Record loadRecord(String key, RecordFieldAdapter recordFieldAdapter);

  @Nonnull public Collection<Record> loadRecords(Collection<String> keys, RecordFieldAdapter recordFieldAdapter) {
    List<Record> records = new ArrayList<>(keys.size());
    for (String key : keys) {
      final Record record = loadRecord(key,recordFieldAdapter);
      if (record != null) {
        records.add(record);
      }
    }
    return records;
  }

  @Nonnull public abstract Set<String> merge(Record record, RecordFieldAdapter recordFieldAdapter);

  @Nonnull public Set<String> merge(Collection<Record> recordSet, RecordFieldAdapter recordFieldAdapter) {
    Set<String> aggregatedDependentKeys = new LinkedHashSet<>();
    for (Record record : recordSet) {
      aggregatedDependentKeys.addAll(merge(record, recordFieldAdapter));
    }
    return aggregatedDependentKeys;
  }

}
