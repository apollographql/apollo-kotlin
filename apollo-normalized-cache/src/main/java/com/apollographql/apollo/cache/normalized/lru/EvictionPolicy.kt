package com.apollographql.apollo.cache.normalized.lru

import com.apollographql.apollo.api.internal.Optional
import java.util.concurrent.TimeUnit

/**
 * Controls how long a [com.apollographql.apollo.cache.normalized.Record] will
 * stay in a [LruNormalizedCache].
 */
class EvictionPolicy internal constructor(
    private val maxSizeBytes: Optional<Long>,
    private val maxEntries: Optional<Long>,
    private val expireAfterAccess: Optional<Long>,
    private val expireAfterAccessTimeUnit: Optional<TimeUnit>,
    private val expireAfterWrite: Optional<Long>,
    private val expireAfterWriteTimeUnit: Optional<TimeUnit>
) {

  fun maxSizeBytes(): Optional<Long> {
    return maxSizeBytes
  }

  fun maxEntries(): Optional<Long> {
    return maxEntries
  }

  fun expireAfterAccess(): Optional<Long> {
    return expireAfterAccess
  }

  fun expireAfterAccessTimeUnit(): Optional<TimeUnit> {
    return expireAfterAccessTimeUnit
  }

  fun expireAfterWrite(): Optional<Long> {
    return expireAfterWrite
  }

  fun expireAfterWriteTimeUnit(): Optional<TimeUnit> {
    return expireAfterWriteTimeUnit
  }

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
          Optional.fromNullable(maxSizeBytes),
          Optional.fromNullable(maxEntries),
          Optional.fromNullable(expireAfterAccess),
          Optional.fromNullable(expireAfterAccessTimeUnit),
          Optional.fromNullable(expireAfterWrite),
          Optional.fromNullable(expireAfterWriteTimeUnit)
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