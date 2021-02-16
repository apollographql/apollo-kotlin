package com.apollographql.apollo3.api.cache.http

import java.util.concurrent.TimeUnit

/**
 * Http cache policy factory
 */
object HttpCachePolicy {
  /**
   * Signals the apollo client to fetch the GraphQL query response from the http cache **only**.
   */
  @JvmField
  val CACHE_ONLY = ExpirePolicy(FetchStrategy.CACHE_ONLY)

  /**
   * Signals the apollo client to fetch the GraphQL query response from the network **only**.
   */
  @JvmField
  val NETWORK_ONLY = Policy(FetchStrategy.NETWORK_ONLY, expireTimeout = 0, expireTimeUnit = null, expireAfterRead = false)

  /**
   * Signals the apollo client to first fetch the GraphQL query response from the http cache. If it's not present in the
   * cache response is fetched from the network.
   */
  @JvmField
  val CACHE_FIRST = ExpirePolicy(FetchStrategy.CACHE_FIRST)

  /**
   * Signals the apollo client to first fetch the GraphQL query response from the network. If it fails then fetch the
   * response from the http cache.
   */
  @JvmField
  val NETWORK_FIRST = ExpirePolicy(FetchStrategy.NETWORK_FIRST)

  /**
   * Abstraction for http cache policy configurations
   */
  open class Policy(
      @JvmField val fetchStrategy: FetchStrategy,
      @JvmField val expireTimeout: Long,
      @JvmField val expireTimeUnit: TimeUnit?,
      @JvmField val expireAfterRead: Boolean
  ) {

    fun expireTimeoutMs(): Long = expireTimeUnit?.toMillis(expireTimeout) ?: 0
  }

  /**
   * Cache policy with provided expiration configuration
   */
  class ExpirePolicy : Policy {
    internal constructor(fetchStrategy: FetchStrategy) : super(fetchStrategy, 0, null, false)

    private constructor(
        fetchStrategy: FetchStrategy,
        expireTimeout: Long,
        expireTimeUnit: TimeUnit?,
        expireAfterRead: Boolean
    ) : super(fetchStrategy, expireTimeout, expireTimeUnit, expireAfterRead)

    /**
     * Create new cache policy with expire after timeout configuration. Cached response is treated as expired if it's
     * served date exceeds.
     *
     * @param expireTimeout  expire timeout after which cached response is treated as expired
     * @param expireTimeUnit time unit
     * @return new cache policy
     */
    fun expireAfter(expireTimeout: Long, expireTimeUnit: TimeUnit): ExpirePolicy {
      return ExpirePolicy(fetchStrategy, expireTimeout, expireTimeUnit, expireAfterRead)
    }

    /**
     * Create new cache policy with expire after read configuration. Cached response will be evicted from the cache
     * after it's been read.
     */
    fun expireAfterRead(): ExpirePolicy {
      return ExpirePolicy(fetchStrategy, expireTimeout, expireTimeUnit, true)
    }
  }

  /**
   * Represents different fetch strategies for http request / response cache
   */
  enum class FetchStrategy {
    /**
     * Signals the apollo client to fetch the GraphQL query response from the http cache **only**.
     */
    CACHE_ONLY,

    /**
     * Signals the apollo client to fetch the GraphQL query response from the network **only**.
     */
    NETWORK_ONLY,

    /**
     * Signals the apollo client to first fetch the GraphQL query response from the http cache. If it's not present in
     * the cache response is fetched from the network.
     */
    CACHE_FIRST,

    /**
     * Signals the apollo client to first fetch the GraphQL query response from the network. If it fails then fetch the
     * response from the http cache.
     */
    NETWORK_FIRST
  }
}
