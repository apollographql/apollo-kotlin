package com.apollographql.android.cache.normalized;

import javax.annotation.Nonnull;

import static com.apollographql.android.api.graphql.util.Utils.checkNotNull;

public final class CacheKey {
  public static final CacheKey NO_KEY = new CacheKey("");

  public static CacheKey from(@Nonnull String key) {
    return new CacheKey(checkNotNull(key, "key == null"));
  }

  private final String key;

  private CacheKey(@Nonnull String key) {
    this.key = key;
  }

  public String key() {
    return key;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CacheKey)) return false;

    CacheKey cacheKey = (CacheKey) o;

    return key.equals(cacheKey.key);
  }

  @Override public int hashCode() {
    return key.hashCode();
  }

  @Override public String toString() {
    return key;
  }
}
