package com.apollographql.apollo.cache.http;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * Represents the http cache policies for request/response http cache.
 */
public final class HttpCachePolicy {

  /**
   * Signals the apollo client to only fetch the data from the http cache. If the data is stale, an
   * {@link com.apollographql.apollo.exception.ApolloHttpException} is thrown.
   */
  public static final ExpireAfterTimeoutFactory CACHE_ONLY = new ExpireAfterTimeoutFactory() {
    @Nonnull @Override public HttpCachePolicy obtain(long expireTimeout, @Nonnull TimeUnit expireTimeUnit) {
      return obtain(expireTimeout, expireTimeUnit, false);
    }

    @Nonnull @Override
    public HttpCachePolicy obtain(long expireTimeout, @Nonnull TimeUnit expireTimeUnit, boolean expireAfterRead) {
      return new HttpCachePolicy(FetchStrategy.CACHE_ONLY, expireTimeout,
          checkNotNull(expireTimeUnit, "expireTimeUnit == null"), expireAfterRead);
    }
  };

  /**
   * Signals the apollo client to only fetch the data from the network.
   */
  public static final WriteOnlyFactory NETWORK_ONLY = new WriteOnlyFactory() {
    @Nonnull @Override public HttpCachePolicy obtain() {
      return new HttpCachePolicy(FetchStrategy.NETWORK_ONLY, 0, null, false);
    }
  };

  /**
   * Signals the apollo client to first fetch the data from the http cache. If the data is stale, then it is fetched
   * from the network.
   */
  public static final ExpireAfterTimeoutFactory CACHE_FIRST = new ExpireAfterTimeoutFactory() {
    @Nonnull @Override public HttpCachePolicy obtain(long expireTimeout, @Nonnull TimeUnit expireTimeUnit) {
      return obtain(expireTimeout, expireTimeUnit, false);
    }

    @Nonnull @Override
    public HttpCachePolicy obtain(long expireTimeout, @Nonnull TimeUnit expireTimeUnit, boolean expireAfterRead) {
      return new HttpCachePolicy(FetchStrategy.CACHE_FIRST, expireTimeout,
          checkNotNull(expireTimeUnit, "expireTimeUnit == null"), expireAfterRead);
    }
  };

  /**
   * Signals the apollo client to first fetch the data from the network request. If the network request fails, then the
   * data is fetched from the http cache.
   */
  public static final ExpireAfterTimeoutFactory NETWORK_FIRST = new ExpireAfterTimeoutFactory() {
    @Nonnull @Override public HttpCachePolicy obtain(long expireTimeout, @Nonnull TimeUnit expireTimeUnit) {
      return obtain(expireTimeout, expireTimeUnit, false);
    }

    @Nonnull @Override
    public HttpCachePolicy obtain(long expireTimeout, @Nonnull TimeUnit expireTimeUnit, boolean expireAfterRead) {
      return new HttpCachePolicy(FetchStrategy.NETWORK_FIRST, expireTimeout,
          checkNotNull(expireTimeUnit, "expireTimeUnit == null"), expireAfterRead);
    }
  };

  /**
   * Signals the apollo client to first fetch the data from the network request. If the network request fails, then the
   * data is fetched from the http cache even if it is stale.
   */
  public static final NeverExpireFactory NETWORK_BEFORE_STALE = new NeverExpireFactory() {
    @Nonnull @Override public HttpCachePolicy obtain() {
      return obtain(false);
    }

    @Nonnull @Override public HttpCachePolicy obtain(boolean expireAfterRead) {
      return new HttpCachePolicy(FetchStrategy.NETWORK_BEFORE_STALE, 0, null, expireAfterRead);
    }
  };

  public final FetchStrategy fetchStrategy;
  public final long expireTimeout;
  public final TimeUnit expireTimeUnit;
  public final boolean expireAfterRead;

  private HttpCachePolicy(FetchStrategy fetchStrategy, long expireTimeout, TimeUnit expireTimeUnit,
      boolean expireAfterRead) {
    this.fetchStrategy = fetchStrategy;
    this.expireTimeout = expireTimeout;
    this.expireTimeUnit = expireTimeUnit;
    this.expireAfterRead = expireAfterRead;
  }

  public long expireTimeoutMs() {
    if (expireTimeUnit == null) {
      return 0;
    }
    return expireTimeUnit.toMillis(expireTimeout);
  }

  public interface WriteOnlyFactory {
    /**
     * Obtain new write only cache policy. Cached response will be only written to cache but never read.
     *
     * @return obtained cache policy
     */
    @Nonnull HttpCachePolicy obtain();
  }

  public interface NeverExpireFactory {
    /**
     * Obtain new never expire policy. Cached response is treated as never expired.
     *
     * @return obtained cache policy
     */
    @Nonnull HttpCachePolicy obtain();

    /**
     * Obtain new never expire policy. Cached response is treated as never expired.
     *
     * @param expireAfterRead signals the apollo client to mark the data in the cache stale after it's been read
     * @return obtained cache policy
     */
    @Nonnull HttpCachePolicy obtain(boolean expireAfterRead);
  }

  public interface ExpireAfterTimeoutFactory {
    /**
     * Obtain new expire after timeout policy. Cached response is treated as expired if his served date is exceed
     * specified timeout.
     *
     * @param expireTimeout  expire timeout after which cached response is treated as expired.
     * @param expireTimeUnit time unit
     * @return obtained cache policy
     */
    @Nonnull HttpCachePolicy obtain(long expireTimeout, @Nonnull TimeUnit expireTimeUnit);

    /**
     * Obtain new expire after timeout policy. Cached response is treated as expired if his served date is exceed
     * specified timeout.
     *
     * @param expireTimeout   expire timeout after which cached response is treated as expired.
     * @param expireTimeUnit  time unit
     * @param expireAfterRead signals the apollo client to mark the data in the cache stale after it's been read.
     * @return obtained cache policy
     */
    @Nonnull HttpCachePolicy obtain(long expireTimeout, @Nonnull TimeUnit expireTimeUnit, boolean expireAfterRead);
  }

  /**
   * Represents different fetch strategies for http request / response cache
   */
  public enum FetchStrategy {
    CACHE_ONLY,
    NETWORK_ONLY,
    CACHE_FIRST,
    NETWORK_FIRST,
    NETWORK_BEFORE_STALE
  }
}
