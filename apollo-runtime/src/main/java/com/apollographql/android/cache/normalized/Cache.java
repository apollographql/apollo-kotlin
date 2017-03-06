package com.apollographql.android.cache.normalized;

import com.apollographql.android.api.graphql.Mutation;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

public final class Cache {

  private final CacheStore cacheStore;
  private final CacheKeyResolver cacheKeyResolver;

  private static final String QUERY_ROOT_KEY = "QUERY_ROOT";
  private static final String MUTATION_ROOT_KEY = "MUTATION_ROOT";

  public Cache(CacheStore cacheStore, CacheKeyResolver cacheKeyResolver) {
    this.cacheStore = cacheStore;
    this.cacheKeyResolver = cacheKeyResolver;
  }

  public static String rootKeyForOperation(Operation operation) {
    if (operation instanceof Query) {
      return QUERY_ROOT_KEY;
    } else if (operation instanceof Mutation) {
      return MUTATION_ROOT_KEY;
    }
    throw new IllegalArgumentException("Unknown operation type.");
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
