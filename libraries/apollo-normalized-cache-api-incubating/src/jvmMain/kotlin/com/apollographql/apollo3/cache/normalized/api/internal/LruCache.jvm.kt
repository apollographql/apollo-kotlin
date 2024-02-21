package com.apollographql.apollo3.cache.normalized.api.internal

import org.mobilenativefoundation.store.cache5.CacheBuilder
import kotlin.time.Duration.Companion.milliseconds

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
  private val cache = CacheBuilder<Key, Value>()
      .apply {
        if (maxSize != Int.MAX_VALUE) {
          weigher(maxSize.toLong(), this@JvmLruCache.weigher)
        }
        if (expireAfterMillis >= 0) {
          expireAfterAccess(expireAfterMillis.milliseconds)
        }
      }
      .build()

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
    cache.invalidateAll(keys.toList())
  }

  override fun keys(): Set<Key> {
    // TODO
    return emptySet()
  }

  override fun clear() {
    cache.invalidateAll()
  }

  override fun size(): Int {
    // TODO
    return -1
  }

  override fun dump(): Map<Key, Value> {
    // TODO
    return emptyMap()
  }
}
