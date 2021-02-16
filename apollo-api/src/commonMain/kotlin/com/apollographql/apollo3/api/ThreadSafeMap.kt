package com.apollographql.apollo3.api

expect class ThreadSafeMap<K, V>() {
  fun getOrPut(key: K, defaultValue: () -> V): V
  fun dispose()
}