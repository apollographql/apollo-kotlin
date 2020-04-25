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
    private var maxSizeBytes = Optional.absent<Long>()
    private var maxEntries = Optional.absent<Long>()
    private var expireAfterAccess = Optional.absent<Long>()
    private var expireAfterAccessTimeUnit = Optional.absent<TimeUnit>()
    private var expireAfterWrite = Optional.absent<Long>()
    private var expireAfterWriteTimeUnit = Optional.absent<TimeUnit>()

    fun maxSizeBytes(maxSizeBytes: Long): Builder {
      this.maxSizeBytes = Optional.of(maxSizeBytes)
      return this
    }

    fun maxEntries(maxEntries: Long): Builder {
      this.maxEntries = Optional.of(maxEntries)
      return this
    }

    fun expireAfterAccess(time: Long, timeUnit: TimeUnit): Builder {
      expireAfterAccess = Optional.of(time)
      expireAfterAccessTimeUnit = Optional.of(timeUnit)
      return this
    }

    fun expireAfterWrite(time: Long, timeUnit: TimeUnit): Builder {
      expireAfterWrite = Optional.of(time)
      expireAfterWriteTimeUnit = Optional.of(timeUnit)
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
    val NO_EVICTION = builder().build()
    fun builder(): Builder {
      return Builder()
    }
  }

}