package com.apollographql.apollo;

import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;

import java.util.Map;

import javax.annotation.Nonnull;

public class IdFieldCacheKeyResolver extends CacheKeyResolver {
  @Nonnull @Override
  public CacheKey fromFieldRecordSet(@Nonnull ResponseField field, @Nonnull Map<String, Object> recordSet) {
    return formatCacheKey((String) recordSet.get("id"));
  }

  @Nonnull @Override
  public CacheKey fromFieldArguments(@Nonnull ResponseField field, @Nonnull Operation.Variables variables) {
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
