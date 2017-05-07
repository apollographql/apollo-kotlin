package com.apollographql.apollo;

import com.apollographql.apollo.cache.http.HttpResponseCacheRecord;
import com.apollographql.apollo.cache.http.HttpResponseCacheRecordEditor;
import com.apollographql.apollo.cache.http.HttpCacheStore;

import java.io.IOException;

import javax.annotation.Nonnull;

final class MockHttpCacheStore implements HttpCacheStore {
  HttpCacheStore delegate;

  @Override public HttpResponseCacheRecord cacheRecord(@Nonnull String cacheKey) throws IOException {
    return delegate.cacheRecord(cacheKey);
  }

  @Override public HttpResponseCacheRecordEditor cacheRecordEditor(@Nonnull String cacheKey) throws IOException {
    return delegate.cacheRecordEditor(cacheKey);
  }

  @Override public void remove(@Nonnull String cacheKey) throws IOException {
    delegate.remove(cacheKey);
  }

  @Override public void delete() throws IOException {
    delegate.delete();
  }
}
