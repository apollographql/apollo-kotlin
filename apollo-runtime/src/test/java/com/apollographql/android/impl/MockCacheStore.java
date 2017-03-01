package com.apollographql.android.impl;

import com.apollographql.android.cache.http.ResponseCacheRecord;
import com.apollographql.android.cache.http.ResponseCacheRecordEditor;
import com.apollographql.android.cache.http.ResponseCacheStore;

import java.io.IOException;

import javax.annotation.Nonnull;

final class MockCacheStore implements ResponseCacheStore {
  ResponseCacheStore delegate;

  @Override public ResponseCacheRecord cacheRecord(@Nonnull String cacheKey) throws IOException {
    return delegate.cacheRecord(cacheKey);
  }

  @Override public ResponseCacheRecordEditor cacheRecordEditor(@Nonnull String cacheKey) throws IOException {
    return delegate.cacheRecordEditor(cacheKey);
  }

  @Override public void remove(@Nonnull String cacheKey) throws IOException {
    delegate.remove(cacheKey);
  }

  @Override public void delete() throws IOException {
    delegate.delete();
  }
}
