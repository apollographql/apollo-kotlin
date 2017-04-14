package com.apollographql.apollo.cache.normalized;

import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Query;

import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Resolves a cache key for a JSON object.
 *
 * @param <R> The type representing the JSON Object. Typically, Map<String, Object>.
 */
public abstract class CacheKeyResolver<R> {
  public static final CacheKeyResolver DEFAULT = new CacheKeyResolver<Map<String, Object>>() {
    @Nonnull @Override public CacheKey resolve(@Nonnull Map<String, Object> jsonObject) {
      return CacheKey.NO_KEY;
    }
  };

  public static final CacheKeyResolver<Record> RECORD = new CacheKeyResolver<Record>() {
    @Nonnull @Override public CacheKey resolve(@Nonnull Record record) {
      return CacheKey.from(record.key());
    }
  };

  private static final CacheKey QUERY_ROOT_KEY = CacheKey.from("QUERY_ROOT");
  private static final CacheKey MUTATION_ROOT_KEY = CacheKey.from("MUTATION_ROOT");

  public static CacheKey rootKeyForOperation(@Nonnull Operation operation) {
    if (operation instanceof Query) {
      return QUERY_ROOT_KEY;
    } else if (operation instanceof Mutation) {
      return MUTATION_ROOT_KEY;
    }
    throw new IllegalArgumentException("Unknown operation type.");
  }

  @Nonnull public abstract CacheKey resolve(@Nonnull R objectSource);
}
