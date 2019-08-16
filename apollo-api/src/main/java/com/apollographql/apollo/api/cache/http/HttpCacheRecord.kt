package com.apollographql.apollo.api.cache.http;

import org.jetbrains.annotations.NotNull;

import okio.Source;

public interface HttpCacheRecord {
  @NotNull Source headerSource();

  @NotNull Source bodySource();

  void close();
}
