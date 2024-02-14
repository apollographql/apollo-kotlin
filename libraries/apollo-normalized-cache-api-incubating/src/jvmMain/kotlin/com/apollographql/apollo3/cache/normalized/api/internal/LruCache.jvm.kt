package com.apollographql.apollo3.cache.normalized.api.internal

import com.google.common.cache.CacheBuilder
import java.util.concurrent.TimeUnit

internal actual fun <Key : Any, Value : Any> LruCache(
    maxSize: Int,
    expireAfterMillis: Long,
    weigher: Weigher<Key, Value>,
): LruCache<Key, Value> = JvmLruCache(maxSize, expireAfterMillis, weigher)

private class JvmLruCache<Key : Any, Value : Any>(
    maxSize: Int,
    expireAfterMillis: Long,
    private val weigher: Weigher<Key, Value>,
) : LruCache<Key, Value> {
  private val cache = CacheBuilder.newBuilder()
      .apply {
        if (maxSize != Int.MAX_VALUE) {
          weigher(weigher)
          maximumWeight(maxSize.toLong())
        }
        if (expireAfterMillis >= 0) {
          expireAfterAccess(expireAfterMillis, TimeUnit.MILLISECONDS)
        }
      }
      .build<Key, Value>()

  override fun get(key: Key): Value? {
    return cache.getIfPresent(key)
  }

  override fun set(key: Key, value: Value) {
    cache.put(key, value)
  }

  override fun remove(key: Key): Value? {
    val value = cache.getIfPresent(key)
    cache.invalidate(key)
    return value
  }

  override fun remove(keys: Collection<Key>) {
    cache.invalidateAll(keys)
  }

  override fun keys(): Set<Key> {
    return cache.asMap().keys
  }

  override fun clear() {
    cache.invalidateAll()
  }

  override fun size(): Int {
    return cache.asMap().entries.sumOf { weigher(it.key, it.value) }
  }

  override fun dump(): Map<Key, Value> {
    return cache.asMap()
  }
}
