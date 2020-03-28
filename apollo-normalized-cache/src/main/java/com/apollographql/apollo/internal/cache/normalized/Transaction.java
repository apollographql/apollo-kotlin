package com.apollographql.apollo.internal.cache.normalized;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings("WeakerAccess") public interface Transaction<T, R> {
  @Nullable R execute(T cache);
}
