package com.apollographql.android.cache.normalized;

import javax.annotation.Nullable;

public interface Transaction<T,R> {
  @Nullable R execute(Transactional<T, R> transactional);
}
