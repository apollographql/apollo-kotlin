package com.apollographql.apollo.api.cache.http;

import okio.Source;
import org.jetbrains.annotations.NotNull;

public interface HttpCacheRecord {
  @NotNull Source headerSource();

  @NotNull Source bodySource();

  void close();
}
