package com.apollographql.android.cache;

import java.util.Map;

import javax.annotation.Nullable;

public interface CacheKeyResolver {
  @Nullable String resolve(Map<String, Object> jsonObject);
}
