package com.apollographql.apollo.api

actual class ThreadSafeMap<K, V> {
  private val map = mutableMapOf<K, V>()

  @Synchronized
  actual fun getOrPut(key: K, defaultValue: () -> V): V {
    return map.getOrPut(key, defaultValue)
  }

  actual fun dispose() {
  }
}