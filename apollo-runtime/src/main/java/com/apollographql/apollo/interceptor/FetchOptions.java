package com.apollographql.apollo.interceptor;

import com.apollographql.apollo.api.internal.Utils;
import com.apollographql.apollo.cache.CacheHeaders;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * Immutable options to configure how a {@link ApolloInterceptorChain} will fetch a response.
 */
public final class FetchOptions {

  public final boolean fetchFromCache;
  public final CacheHeaders cacheHeaders;

  public static final FetchOptions NETWORK_ONLY = new FetchOptions(false, CacheHeaders.NONE);

  public FetchOptions(boolean fetchFromCache, @Nonnull CacheHeaders cacheHeaders) {
    this.fetchFromCache = fetchFromCache;
    this.cacheHeaders = Utils.checkNotNull(cacheHeaders);
  }

  private FetchOptions(Builder builder) {
    this(builder.fetchFromCache, builder.cacheHeaders);
  }

  public Builder edit() {
    return new Builder(this);
  }

  /**
   * Returns a new {@link FetchOptions} with {@link #fetchFromCache} true.
   */
  @CheckReturnValue
  public FetchOptions toCacheFetchOptions() {
    return this.edit().fetchFromCache(true).build();
  }

  /**
   * Returns a new {@link FetchOptions} with {@link #fetchFromCache} false.
   */
  @CheckReturnValue
  public FetchOptions toNetworkFetchOptions() {
    return this.edit().fetchFromCache(false).build();
  }

  @CheckReturnValue
  public FetchOptions addCacheHeader(String headerName, String headerValue) {
    return this.edit().addCacheHeader(headerName, headerName).build();
  }

  @CheckReturnValue
  public FetchOptions cacheHeaders(CacheHeaders cacheHeaders) {
    return this.edit().cacheHeaders(cacheHeaders).build();
  }

  public static final class Builder {
    private boolean fetchFromCache;
    private CacheHeaders cacheHeaders;

    public Builder() {
      this(NETWORK_ONLY);
    }

    public Builder(FetchOptions fetchOptions) {
      this.fetchFromCache = fetchOptions.fetchFromCache;
      this.cacheHeaders = fetchOptions.cacheHeaders;
    }

    public Builder fetchFromCache(boolean fetchFromCache) {
      this.fetchFromCache = fetchFromCache;
      return this;
    }

    public Builder cacheHeaders(CacheHeaders cacheHeaders) {
      this.cacheHeaders = cacheHeaders;
      return this;
    }

    public Builder addCacheHeader(String headerName, String headerValue) {
      this.cacheHeaders = this.cacheHeaders.toBuilder().addHeader(headerName, headerValue).build();
      return this;
    }

    public FetchOptions build() {
      return new FetchOptions(this);
    }
  }

}
