package com.apollographql.android.cache;

import javax.annotation.Nonnull;

import okhttp3.Response;

public interface EvictionStrategy {
  boolean isStale(@Nonnull Response response);
}
