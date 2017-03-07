package com.apollographql.android.cache.normalized;

import java.util.Collection;

import javax.annotation.Nonnull;

import static com.apollographql.android.impl.util.Utils.checkNotNull;

public final class RealCache implements Cache {
  private final CacheStore cacheStore;
  private final CacheKeyResolver cacheKeyResolver;

  public RealCache(@Nonnull CacheStore cacheStore, @Nonnull CacheKeyResolver cacheKeyResolver) {
    this.cacheStore = cacheStore;
    this.cacheKeyResolver = cacheKeyResolver;
  }

  @Override @Nonnull public ResponseNormalizer responseNormalizer() {
    return new ResponseNormalizer(cacheKeyResolver);
  }

  @Override public void write(@Nonnull Record record) {
    cacheStore.merge(checkNotNull(record, "record == null"));
  }

  @Override public void write(@Nonnull Collection<Record> recordSet) {
    cacheStore.merge(checkNotNull(recordSet, "recordSet == null"));
  }

  @Override public Record read(@Nonnull String key) {
    return cacheStore.loadRecord(checkNotNull(key, "key == null"));
  }

  @Override public Collection<Record> read(@Nonnull Collection<String> keys) {
    return cacheStore.loadRecords(checkNotNull(keys, "keys == null"));
  }
}
