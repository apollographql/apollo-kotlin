package com.apollographql.apollo.cache.normalized;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;

import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Resolves a cache key for a JSON object.
 */
public abstract class CacheKeyResolver {
  public static final CacheKey ROOT_RECORD_KEY = CacheKey.from("QUERY_ROOT");

  public static final CacheKeyResolver DEFAULT = new CacheKeyResolver() {
    @Nonnull @Override
    public CacheKey fromFieldRecordSet(@Nonnull ResponseField field, @Nonnull Map<String, Object> recordSet) {
      return CacheKey.NO_KEY;
    }

    @Nonnull @Override
    public CacheKey fromFieldArguments(@Nonnull ResponseField field, @Nonnull Operation.Variables variables) {
      return CacheKey.NO_KEY;
    }
  };

  @Nonnull public abstract CacheKey fromFieldRecordSet(@Nonnull ResponseField field,
      @Nonnull Map<String, Object> recordSet);

  @Nonnull public abstract CacheKey fromFieldArguments(@Nonnull ResponseField field,
      @Nonnull Operation.Variables variables);
}
