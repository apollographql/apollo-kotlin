package com.apollographql.apollo3.api.internal

/**
 * A simple Map that can be used as a cache in multithreaded scenarios
 *
 * On the JVM it will use a lock. On native, it will use a [Stately-like](https://github.com/touchlab/Stately) pattern to make sure the Map is always modified from
 * the same thread
 */
expect class ThreadSafeMap<K, V>() {
  fun getOrPut(key: K, defaultValue: () -> V): V
  fun dispose()
}