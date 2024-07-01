package com.apollographql.apollo.api

/**
 * A helper class to make it easier to build Maps from the java codegen
 */
class ImmutableMapBuilder<K, V> {
  private val map: MutableMap<K, V> = mutableMapOf<K, V>()

  fun put(key: K, value: V) = apply {
    map[key] = value
  }

  fun build(): Map<K, V> {
    return map
  }
}