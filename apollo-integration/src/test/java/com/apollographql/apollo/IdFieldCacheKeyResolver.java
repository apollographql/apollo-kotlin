package com.apollographql.apollo;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class IdFieldCacheKeyResolver extends CacheKeyResolver {

  @NotNull @Override
  public CacheKey fromFieldRecordSet(@NotNull ResponseField field, @NotNull Map<String, Object> recordSet) {
    Object id = recordSet.get("id");
    if (id != null) {
      return formatCacheKey(id.toString());
    } else {
      return formatCacheKey(null);
    }
  }

  @NotNull @Override
  public CacheKey fromFieldArguments(@NotNull ResponseField field, @NotNull Operation.Variables variables) {
    Object id = field.resolveArgument("id", variables);
    if (id != null) {
      return formatCacheKey(id.toString());
    } else {
      return formatCacheKey(null);
    }
  }

  private CacheKey formatCacheKey(String id) {
    if (id == null || id.isEmpty()) {
      return CacheKey.NO_KEY;
    } else {
      return new CacheKey(id);
    }
  }
}
