package com.apollographql.android.cache.http;

import java.io.IOException;

import javax.annotation.Nonnull;

public interface ResponseCacheStore {
  ResponseCacheRecord cacheRecord(@Nonnull String cacheKey) throws IOException;

  ResponseCacheRecordEditor cacheRecordEditor(@Nonnull String cacheKey) throws IOException;

  void remove(@Nonnull String cacheKey) throws IOException;

  void delete() throws IOException;
}
