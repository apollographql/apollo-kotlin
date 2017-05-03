package com.apollographql.apollo.internal.cache.normalized;

import com.apollographql.apollo.cache.normalized.Record;

import java.util.Collection;
import java.util.Set;

public interface WriteableStore extends ReadableStore {

  Set<String> merge(Collection<Record> recordCollection);

  void clearAll();

}
