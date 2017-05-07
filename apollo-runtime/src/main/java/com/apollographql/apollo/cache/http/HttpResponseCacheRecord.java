package com.apollographql.apollo.cache.http;

import javax.annotation.Nonnull;

import okio.Source;

public interface HttpResponseCacheRecord {
  @Nonnull Source headerSource();

  @Nonnull Source bodySource();

  void close();
}
