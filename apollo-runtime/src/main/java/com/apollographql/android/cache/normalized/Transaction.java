package com.apollographql.android.cache.normalized;

import javax.annotation.Nullable;

@SuppressWarnings("WeakerAccess") public interface Transaction<T, R> {
  @Nullable R execute(Transactional<T, R> transactional);
}
