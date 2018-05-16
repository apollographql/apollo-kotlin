package com.apollographql.apollo.internal.cache.normalized;

import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.Record;

import java.util.Collection;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

public interface WriteableStore extends ReadableStore {

  Set<String> merge(@NotNull Collection<Record> recordCollection, @NotNull CacheHeaders cacheHeaders);
  Set<String> merge(Record record, @NotNull CacheHeaders cacheHeaders);
}
