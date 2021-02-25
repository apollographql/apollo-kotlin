package com.apollographql.apollo3.api.internal

actual class ThreadSafeMap<K, V> {
  private val map = mutableMapOf<K, V>()
  actual fun getOrPut(key: K, defaultValue: () -> V): V {
    return map.getOrPut(key, defaultValue)
  }
  actual fun dispose() {
  }
}