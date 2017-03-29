package com.apollographql.apollo.cache.http;

import javax.annotation.Nonnull;

import okhttp3.Response;

public interface EvictionStrategy {
  boolean isStale(@Nonnull Response response);
}
