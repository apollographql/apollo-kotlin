package com.apollographql.apollo.cache.normalized.internal;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings("WeakerAccess") public interface Transaction<T, R> {
  @Nullable R execute(T cache);
}
