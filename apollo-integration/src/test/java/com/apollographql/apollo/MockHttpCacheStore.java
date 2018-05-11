package com.apollographql.apollo;

import com.apollographql.apollo.api.cache.http.HttpCacheRecord;
import com.apollographql.apollo.api.cache.http.HttpCacheRecordEditor;
import com.apollographql.apollo.api.cache.http.HttpCacheStore;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;

final class MockHttpCacheStore implements HttpCacheStore {
  HttpCacheStore delegate;

  @Override public HttpCacheRecord cacheRecord(@NotNull String cacheKey) throws IOException {
    return delegate.cacheRecord(cacheKey);
  }

  @Override public HttpCacheRecordEditor cacheRecordEditor(@NotNull String cacheKey) throws IOException {
    return delegate.cacheRecordEditor(cacheKey);
  }

  @Override public void remove(@NotNull String cacheKey) throws IOException {
    delegate.remove(cacheKey);
  }

  @Override public void delete() throws IOException {
    delegate.delete();
  }
}
