package com.apollographql.apollo.internal.cache.normalized;

import javax.annotation.Nullable;

@SuppressWarnings("WeakerAccess") public interface Transaction<T, R> {
  @Nullable R execute(T cache);
}
