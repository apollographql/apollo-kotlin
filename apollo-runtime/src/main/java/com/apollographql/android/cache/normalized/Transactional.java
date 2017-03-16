package com.apollographql.android.cache.normalized;

public interface Transactional<T, R> {
  R call(T cache);
}
