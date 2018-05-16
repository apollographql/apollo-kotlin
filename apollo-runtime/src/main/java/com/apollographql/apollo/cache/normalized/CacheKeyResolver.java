package com.apollographql.apollo.cache.normalized;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

/**
 * Resolves a cache key for a JSON object.
 */
public abstract class CacheKeyResolver {
  private static final CacheKey ROOT_CACHE_KEY = CacheKey.from("QUERY_ROOT");

  public static final CacheKeyResolver DEFAULT = new CacheKeyResolver() {
    @NotNull @Override
    public CacheKey fromFieldRecordSet(@NotNull ResponseField field, @NotNull Map<String, Object> recordSet) {
      return CacheKey.NO_KEY;
    }

    @NotNull @Override
    public CacheKey fromFieldArguments(@NotNull ResponseField field, @NotNull Operation.Variables variables) {
      return CacheKey.NO_KEY;
    }
  };

  @SuppressWarnings("unused")
  public static CacheKey rootKeyForOperation(@NotNull Operation operation) {
    return ROOT_CACHE_KEY;
  }

  @NotNull public abstract CacheKey fromFieldRecordSet(@NotNull ResponseField field,
      @NotNull Map<String, Object> recordSet);

  @NotNull public abstract CacheKey fromFieldArguments(@NotNull ResponseField field,
      @NotNull Operation.Variables variables);
}
