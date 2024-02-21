package com.apollographql.apollo3.cache.normalized.api.internal.store

/**
 * @return Weight of a cache entry. Must be non-negative. There is no unit for entry weights. Rather, they are simply relative to each other.
 */
typealias Weigher <Key, Value> = (key: Key, value: Value) -> Int
