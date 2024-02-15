package com.apollographql.apollo3.cache.normalized.api.internal

/**
 * A naive concurrent map implementation that uses a read-write lock to synchronize access to the underlying map.
 * This is used for native and JS/Wasm while the JVM uses the built-in `ConcurrentHashMap`.
 */
internal class CommonConcurrentMap<K, V> : MutableMap<K, V> {
  private val lock = Lock()
  private val map = mutableMapOf<K, V>()

  override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
    get() = lock.read { map.entries }

  override val keys: MutableSet<K>
    get() = lock.read { map.keys }

  override val size: Int
    get() = lock.read { map.size }

  override val values: MutableCollection<V>
    get() = lock.read { map.values }

  override fun clear() {
    lock.write { map.clear() }
  }

  override fun isEmpty(): Boolean {
    return lock.read { map.isEmpty() }
  }

  override fun remove(key: K): V? {
    return lock.write { map.remove(key) }
  }

  override fun putAll(from: Map<out K, V>) {
    lock.write { map.putAll(from) }
  }

  override fun put(key: K, value: V): V? {
    return lock.write { map.put(key, value) }
  }

  override fun get(key: K): V? {
    return lock.read { map[key] }
  }

  override fun containsValue(value: V): Boolean {
    return lock.read { map.containsValue(value) }
  }

  override fun containsKey(key: K): Boolean {
    return lock.read { map.containsKey(key) }
  }
}
