package com.apollographql.apollo;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

public class IdFieldCacheKeyResolver extends CacheKeyResolver {
  @NotNull @Override
  public CacheKey fromFieldRecordSet(@NotNull ResponseField field, @NotNull Map<String, Object> recordSet) {
    return formatCacheKey((String) recordSet.get("id"));
  }

  @NotNull @Override
  public CacheKey fromFieldArguments(@NotNull ResponseField field, @NotNull Operation.Variables variables) {
    return formatCacheKey((String) field.resolveArgument("id", variables));
  }

  private CacheKey formatCacheKey(String id) {
    if (id == null || id.isEmpty()) {
      return CacheKey.NO_KEY;
    } else {
      return CacheKey.from(id);
    }
  }
}
