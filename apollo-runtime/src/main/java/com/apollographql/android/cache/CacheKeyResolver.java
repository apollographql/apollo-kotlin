package com.apollographql.android.cache;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface CacheKeyResolver {
  @Nullable String resolve(@Nonnull Map<String, Object> jsonObject);
}
