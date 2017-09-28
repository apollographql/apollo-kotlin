package com.apollographql.apollo.api.cache.http;

import javax.annotation.Nonnull;

import okio.Source;

public interface HttpCacheRecord {
  @Nonnull Source headerSource();

  @Nonnull Source bodySource();

  void close();
}
