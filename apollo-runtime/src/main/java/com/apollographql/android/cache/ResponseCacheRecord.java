package com.apollographql.android.cache;

import javax.annotation.Nonnull;

import okio.Source;

public interface ResponseCacheRecord {
  @Nonnull Source headerSource();

  @Nonnull Source bodySource();

  void close();
}
