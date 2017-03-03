package com.apollographql.android.cache.normalized;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface CacheKeyResolver {
  @Nullable String resolve(@Nonnull Map<String, Object> jsonObject);
}
