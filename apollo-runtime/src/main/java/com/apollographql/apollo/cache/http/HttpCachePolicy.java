package com.apollographql.apollo.cache.http;

import com.apollographql.apollo.internal.cache.http.HttpCacheFetchStrategy;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * Http cache policy factory
 */
public final class HttpCachePolicy {

  /**
   * Signals the apollo client to fetch the GraphQL query response from the http cache <b>only</b>.
   */
  public static final ExpirePolicy CACHE_ONLY = new ExpirePolicy(HttpCacheFetchStrategy.CACHE_ONLY);

  /**
   * Signals the apollo client to fetch the GraphQL query response from the network <b>only</b>.
   */
  public static final Policy NETWORK_ONLY = new Policy(HttpCacheFetchStrategy.NETWORK_ONLY, 0, null, false);

  /**
   * Signals the apollo client to first fetch the GraphQL query response from the http cache. If it's not present in the
   * cache response is fetched from the network.
   */
  public static final ExpirePolicy CACHE_FIRST = new ExpirePolicy(HttpCacheFetchStrategy.CACHE_FIRST);

  /**
   * Signals the apollo client to first fetch the GraphQL query response from the network. If it fails then fetch the
   * response from the http cache.
   */
  public static final ExpirePolicy NETWORK_FIRST = new ExpirePolicy(HttpCacheFetchStrategy.NETWORK_FIRST);

  private HttpCachePolicy() {
  }

  /**
   * Abstraction for http cache policy configurations
   */
  public static class Policy {
    public final HttpCacheFetchStrategy fetchStrategy;
    public final long expireTimeout;
    public final TimeUnit expireTimeUnit;
    public final boolean expireAfterRead;

    Policy(HttpCacheFetchStrategy fetchStrategy, long expireTimeout, TimeUnit expireTimeUnit,
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
  }

  /**
   * Cache policy with provided expiration configuration
   */
  public static final class ExpirePolicy extends Policy {
    ExpirePolicy(HttpCacheFetchStrategy fetchStrategy) {
      super(fetchStrategy, 0, null, false);
    }

    private ExpirePolicy(HttpCacheFetchStrategy fetchStrategy, long expireTimeout, TimeUnit expireTimeUnit,
        boolean expireAfterRead) {
      super(fetchStrategy, expireTimeout, expireTimeUnit, expireAfterRead);
    }

    /**
     * Create new cache policy with expire after timeout configuration. Cached response is treated as expired if it's
     * served date exceeds.
     *
     * @param expireTimeout  expire timeout after which cached response is treated as expired
     * @param expireTimeUnit time unit
     * @return new cache policy
     */
    public ExpirePolicy expireAfter(long expireTimeout, @Nonnull TimeUnit expireTimeUnit) {
      return new ExpirePolicy(fetchStrategy, expireTimeout, checkNotNull(expireTimeUnit), expireAfterRead);
    }

    /**
     * Create new cache policy with expire after read configuration. Cached response will be evicted from the cache
     * after it's been read.
     */
    public ExpirePolicy expireAfterRead() {
      return new ExpirePolicy(fetchStrategy, expireTimeout, expireTimeUnit, true);
    }
  }
}
