package com.apollographql.apollo.cache.normalized.internal;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;

import org.jetbrains.annotations.NotNull;

public interface CacheKeyBuilder {
  @NotNull String build(@NotNull ResponseField field, @NotNull Operation.Variables variables);
}
