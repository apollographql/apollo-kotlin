package com.apollographql.android.cache.normalized.sql;


import com.apollographql.android.cache.normalized.CacheStore;
import com.apollographql.android.cache.normalized.Record;

import javax.annotation.Nullable;

public class SqlStore extends CacheStore {
  @Nullable @Override public Record loadRecord(String key) {
    return null;
  }

  @Override public void merge(Record object) {

  }
}
