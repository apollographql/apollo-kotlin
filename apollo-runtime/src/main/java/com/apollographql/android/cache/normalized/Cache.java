package com.apollographql.android.cache.normalized;

import com.apollographql.android.impl.ResponseNormalizer;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

public final class Cache {

  private final CacheStore cacheStore;
  private final CacheKeyResolver cacheKeyResolver;

  public Cache(CacheStore cacheStore, CacheKeyResolver cacheKeyResolver) {
    this.cacheStore = cacheStore;
    this.cacheKeyResolver = cacheKeyResolver;
  }

  public CacheStore cacheStore() {
    return cacheStore;
  }

  public CacheKeyResolver cacheKeyResolver() {
    return cacheKeyResolver;
  }

  public ResponseNormalizer responseNormalizer() {
    return new ResponseNormalizer(cacheKeyResolver);
  }

  public static final Cache NO_OP_NORMALIZED_CACHE = new Cache(new CacheStore() {
    @Override public Record loadRecord(String key) {
      return null;
    }

    @Override public void merge(Record object) {

    }

    @Override public void merge(Collection<Record> recordSet) {

    }

  }, new CacheKeyResolver() {
    @Nullable @Override public String resolve(Map<String, Object> jsonObject) {
      return null;
    }
  });

  //Todo: add interceptor for reading (https://github.com/apollographql/apollo-android/issues/266)

}
