package com.apollographql.android.cache.normalized;

import com.apollographql.android.api.graphql.Mutation;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class CacheKeyResolver {
  public static final CacheKeyResolver DEFAULT = new CacheKeyResolver() {
    @Nullable @Override public String resolve(@Nonnull Map<String, Object> jsonObject) {
      return null;
    }
  };
  private static final String QUERY_ROOT_KEY = "QUERY_ROOT";
  private static final String MUTATION_ROOT_KEY = "MUTATION_ROOT";

  public static String rootKeyForOperation(@Nonnull Operation operation) {
    if (operation instanceof Query) {
      return QUERY_ROOT_KEY;
    } else if (operation instanceof Mutation) {
      return MUTATION_ROOT_KEY;
    }
    throw new IllegalArgumentException("Unknown operation type.");
  }

  @Nullable public abstract String resolve(@Nonnull Map<String, Object> jsonObject);
}
