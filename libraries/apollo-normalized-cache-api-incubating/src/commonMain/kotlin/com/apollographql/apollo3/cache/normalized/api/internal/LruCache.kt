package com.apollographql.apollo3.cache.normalized.api.internal

internal interface LruCache<Key: Any, Value: Any> {
  operator fun get(key: Key): Value?

  operator fun set(key: Key, value: Value)

  fun remove(key: Key): Value?

  fun keys(): Set<Key>

  fun remove(keys: Collection<Key>)

  fun clear()

  fun size(): Int

  fun dump(): Map<Key, Value>
}

internal typealias Weigher<Key, Value> = (Key, Value) -> Int

internal expect fun <Key:Any, Value:Any> LruCache(maxSize: Int, expireAfterMillis: Long, weigher: Weigher<Key, Value>): LruCache<Key, Value>
