package com.apollographql.apollo3.cache.normalized.api.internal

import com.apollographql.apollo3.mpp.currentTimeMillis

/**
 * Multiplatform thread-safe LRU cache implementation.
 *
 * Implementation is based on usage of [LinkedHashMap] as a container for the cache and custom
 * double linked queue to track LRU property.
 *
 * @param maxSize maximum size of the cache, can be anything bytes, number of entries etc.
 * @param weigher to be called to calculate the estimated size (weight) of the cache entry defined by its [Key] and [Value].
 * @param expireAfterMillis time in milliseconds after which the cache entry is considered expired, or -1 for no timeout.
 *
 * Cache trim performed only on new entry insertion.
 */
internal class CommonLruCache<Key : Any, Value : Any>(
    private val maxSize: Int,
    private val expireAfterMillis: Long,
    private val weigher: Weigher<Key, Value>,
) : LruCache<Key, Value> {
  private val cache = LinkedHashMap<Key, Node<Key, Value>>(0, 0.75f)
  private var headNode: Node<Key, Value>? = null
  private var tailNode: Node<Key, Value>? = null
  private var size: Int = 0

  private val lock = Lock()

  override operator fun get(key: Key): Value? {
    return lock.write {
      val node = cache[key]
      if (node != null) {
        if (node.isExpired) {
          removeUnsafe(key)
          return@write null
        }
        moveNodeToHead(node)
      }
      node?.value
    }
  }

  override operator fun set(key: Key, value: Value) {
    lock.write {
      val node = cache[key]
      if (node == null) {
        cache[key] = addNode(key, value)
      } else {
        node.value = value
        node.cachedAtMillis = currentTimeMillis()
        moveNodeToHead(node)
      }

      trim()
    }
  }

  override fun remove(key: Key): Value? {
    return lock.write { removeUnsafe(key) }
  }

  override fun keys() = lock.read { cache.keys }

  private fun removeUnsafe(key: Key): Value? {
    val nodeToRemove = cache.remove(key)
    val value = nodeToRemove?.value
    if (nodeToRemove != null) {
      unlinkNode(nodeToRemove)
    }
    return value
  }

  override fun remove(keys: Collection<Key>) {
    lock.write {
      keys.forEach { key -> removeUnsafe(key) }
    }
  }

  override fun clear() {
    return lock.write {
      cache.clear()
      headNode = null
      tailNode = null
      size = 0
    }
  }

  override fun size(): Int {
    return lock.read { size }
  }

  override fun dump(): Map<Key, Value> {
    return lock.read {
      cache
          .filterValues { it.value != null && !it.isExpired }
          .mapValues { (_, value) -> value.value!! }
    }
  }

  private fun trim() {
    var nodeToRemove = tailNode
    while (nodeToRemove != null && size > maxSize) {
      cache.remove(nodeToRemove.key)
      unlinkNode(nodeToRemove)
      nodeToRemove = tailNode
    }
  }

  private fun addNode(key: Key, value: Value): Node<Key, Value> {
    val node = Node(
        key = key,
        value = value,
        cachedAtMillis = currentTimeMillis(),
        next = headNode,
        prev = null,
    )

    headNode = node

    if (node.next == null) {
      tailNode = headNode
    } else {
      node.next?.prev = headNode
    }

    size += weigher(key, value)

    return node
  }

  private fun moveNodeToHead(node: Node<Key, Value>) {
    if (node.prev == null) {
      return
    }

    node.prev?.next = node.next

    if (node.next == null) {
      tailNode = node.prev
    } else {
      node.next?.prev = node.prev
    }

    node.next = headNode
    node.prev = null

    headNode?.prev = node
    headNode = node
  }

  private fun unlinkNode(node: Node<Key, Value>) {
    if (node.prev == null) {
      this.headNode = node.next
    } else {
      node.prev?.next = node.next
    }

    if (node.next == null) {
      this.tailNode = node.prev
    } else {
      node.next?.prev = node.prev
    }

    size -= weigher(node.key!!, node.value!!)

    node.key = null
    node.value = null
    node.cachedAtMillis = null
    node.next = null
    node.prev = null
  }

  private class Node<Key, Value>(
      var key: Key?,
      var value: Value?,
      var cachedAtMillis: Long?,
      var next: Node<Key, Value>?,
      var prev: Node<Key, Value>?,
  )

  private val Node<Key, Value>.isExpired: Boolean
    get() {
      return if (expireAfterMillis < 0) {
        false
      } else {
        val cachedAtMillis = this.cachedAtMillis ?: 0
        currentTimeMillis() - cachedAtMillis >= expireAfterMillis
      }
    }
}
