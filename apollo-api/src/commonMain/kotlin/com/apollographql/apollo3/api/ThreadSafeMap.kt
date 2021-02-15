package com.apollographql.apollo.api

expect class ThreadSafeMap<K, V>() {
  fun getOrPut(key: K, defaultValue: () -> V): V
}