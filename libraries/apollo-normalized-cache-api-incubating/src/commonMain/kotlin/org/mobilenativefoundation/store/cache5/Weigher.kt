package org.mobilenativefoundation.store.cache5

/**
 * @return Weight of a cache entry. Must be non-negative. There is no unit for entry weights. Rather, they are simply relative to each other.
 */
typealias Weigher <Key, Value> = (key: Key, value: Value) -> Int
