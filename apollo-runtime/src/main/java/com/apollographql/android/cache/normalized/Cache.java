package com.apollographql.android.cache.normalized;

import com.apollographql.android.api.graphql.Mutation;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.android.impl.util.Utils.checkNotNull;

public final class Cache {
  private static final String QUERY_ROOT_KEY = "QUERY_ROOT";
  private static final String MUTATION_ROOT_KEY = "MUTATION_ROOT";

  private final CacheStore cacheStore;
  private final CacheKeyResolver cacheKeyResolver;

  public Cache(@Nonnull CacheStore cacheStore, @Nonnull CacheKeyResolver cacheKeyResolver) {
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

  public ResponseNormalizer responseNormalizer() {
    return new ResponseNormalizer(cacheKeyResolver);
  }

  public void write(@Nonnull Record record) {
    cacheStore.merge(checkNotNull(record, "record == null"));
  }

  public void write(@Nonnull Collection<Record> recordSet) {
    cacheStore.merge(checkNotNull(recordSet, "recordSet == null"));
  }

  public Record read(@Nonnull String key) {
    return cacheStore.loadRecord(checkNotNull(key, "key == null"));
  }

  public Collection<Record> read(@Nonnull Collection<String> keys) {
    return cacheStore.loadRecords(checkNotNull(keys, "keys == null"));
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
