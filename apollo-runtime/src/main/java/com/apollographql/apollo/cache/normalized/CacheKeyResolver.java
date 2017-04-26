package com.apollographql.apollo.cache.normalized;

import com.apollographql.apollo.api.Field;
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
public abstract class CacheKeyResolver {
  public static final CacheKeyResolver DEFAULT = new CacheKeyResolver() {
    @Nonnull @Override public CacheKey resolve(@Nonnull Field field, @Nonnull Map<String, Object> arguments) {
      return CacheKey.NO_KEY;
    }
  };
  public static final CacheKey QUERY_ROOT_KEY = CacheKey.from("QUERY_ROOT");
  public static final CacheKey MUTATION_ROOT_KEY = CacheKey.from("MUTATION_ROOT");

  public static CacheKey rootKeyForOperation(@Nonnull Operation operation) {
    if (operation instanceof Query) {
      return QUERY_ROOT_KEY;
    } else if (operation instanceof Mutation) {
      return MUTATION_ROOT_KEY;
    }
    throw new IllegalArgumentException("Unknown operation type.");
  }

  @Nonnull public abstract CacheKey resolve(@Nonnull Field field, @Nonnull Map<String, Object> arguments);
}
