package com.apollographql.apollo.cache.normalized.internal

internal typealias Weigher<Key, Value> = (Key, Value?) -> Int

internal class LruCache<Key, Value>(
    private val maxSize: Long,
    private val weigher: Weigher<Key, Value> = { _, _ -> 1 }
) {
  private val cache = LinkedHashMap<Key, Node<Key, Value>>(0, 0.75f)
  private var headNode: Node<Key, Value>? = null
  private var tailNode: Node<Key, Value>? = null
  var size: Long = 0
    private set

  operator fun get(key: Key): Value? {
    val node = cache[key]
    if (node != null) {
      moveNodeToHead(node)
    }
    return node?.value
  }

  operator fun set(key: Key, value: Value?) {
    val node = cache[key]
    if (node == null) {
      cache[key] = addNode(key, value)
    } else {
      node.value = value
      moveNodeToHead(node)
    }

    trim()
  }

  fun remove(key: Key): Boolean {
    val nodeToRemove = cache.remove(key)
    if (nodeToRemove != null) {
      var nextEntry = headNode
      while (nextEntry != null) {
        if (nextEntry.value == nodeToRemove.value) {
          unlinkNode(nextEntry)
          return true
        }
        nextEntry = nextEntry.next
      }
    }

    return nodeToRemove != null
  }

  fun clear() {
    cache.clear()
    headNode = null
    tailNode = null
    size = 0
  }

  fun dump(): Map<Key, Value?> {
    return cache.mapValues { (_, value) -> value.value }
  }

  private fun trim() {
    var nodeToRemove = tailNode
    while (nodeToRemove != null && size > maxSize) {
      cache.remove(nodeToRemove.key)
      unlinkNode(nodeToRemove)
      nodeToRemove = tailNode
    }
  }

  private fun addNode(key: Key, value: Value?): Node<Key, Value> {
    val node = Node(
        key = key,
        value = value,
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
    node.next?.prev = node.prev

    node.next = headNode?.next
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

    size -= weigher(node.key!!, node.value)

    node.key = null
    node.value = null
    node.next = null
    node.prev = null
  }

  private class Node<Key, Value>(
      var key: Key?,
      var value: Value?,
      var next: Node<Key, Value>?,
      var prev: Node<Key, Value>?,
  )
}
