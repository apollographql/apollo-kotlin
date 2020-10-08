package com.apollographql.apollo.cache.normalized.lru

import java.util.concurrent.TimeUnit

/**
 * Controls how long a [com.apollographql.apollo.cache.normalized.Record] will
 * stay in a [LruNormalizedCache].
 */
class EvictionPolicy internal constructor(
    val maxSizeBytes: Long? = null,
    val maxEntries: Long? = null,
    val expireAfterAccess: Long? = null,
    val expireAfterAccessTimeUnit: TimeUnit? = null,
    val expireAfterWrite: Long? = null,
    val expireAfterWriteTimeUnit: TimeUnit? = null
) {

  class Builder internal constructor() {
    private var maxSizeBytes: Long? = null
    private var maxEntries: Long? = null
    private var expireAfterAccess: Long? = null
    private var expireAfterAccessTimeUnit: TimeUnit? = null
    private var expireAfterWrite: Long? = null
    private var expireAfterWriteTimeUnit: TimeUnit? = null

    fun maxSizeBytes(maxSizeBytes: Long): Builder {
      this.maxSizeBytes = maxSizeBytes
      return this
    }

    fun maxEntries(maxEntries: Long): Builder {
      this.maxEntries = maxEntries
      return this
    }

    fun expireAfterAccess(time: Long, timeUnit: TimeUnit): Builder {
      expireAfterAccess = time
      expireAfterAccessTimeUnit = timeUnit
      return this
    }

    fun expireAfterWrite(time: Long, timeUnit: TimeUnit): Builder {
      expireAfterWrite = time
      expireAfterWriteTimeUnit = timeUnit
      return this
    }

    fun build(): EvictionPolicy {
      return EvictionPolicy(
          maxSizeBytes,
          maxEntries,
          expireAfterAccess,
          expireAfterAccessTimeUnit,
          expireAfterWrite,
          expireAfterWriteTimeUnit
      )
    }
  }

  companion object {

    @JvmField
    val NO_EVICTION = builder().build()

    @JvmStatic
    fun builder(): Builder {
      return Builder()
    }
  }

}
