package com.apollographql.apollo3.cache.normalized.api.internal

import com.apollographql.apollo3.cache.normalized.api.internal.store.CacheBuilder
import kotlin.time.Duration.Companion.milliseconds

internal class LruCache<Key : Any, Value : Any>(
    maxSize: Int,
    expireAfterMillis: Long,
    private val weigher: Weigher<Key, Value>,
) {
  private val cache = CacheBuilder<Key, Value>()
      .apply {
        if (maxSize != Int.MAX_VALUE) {
          weigher(maxSize.toLong(), this@LruCache.weigher)
        }
        if (expireAfterMillis >= 0) {
          expireAfterAccess(expireAfterMillis.milliseconds)
        }
      }
      .build()

  operator fun get(key: Key): Value? {
    return cache.getIfPresent(key)
  }

  operator fun set(key: Key, value: Value) {
    cache.put(key, value)
  }

  fun remove(key: Key): Value? {
    val value = cache.getIfPresent(key)
    cache.invalidate(key)
    return value
  }

  fun clear() {
    cache.invalidateAll()
  }

  fun weight(): Int {
    return cache.getAllPresent().entries.sumOf { weigher(it.key, it.value) }
  }

  fun asMap(): Map<Key, Value> {
    return cache.getAllPresent()
  }
}

internal typealias Weigher<Key, Value> = (Key, Value) -> Int
