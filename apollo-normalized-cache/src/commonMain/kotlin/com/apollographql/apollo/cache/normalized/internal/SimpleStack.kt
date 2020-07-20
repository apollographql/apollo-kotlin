package com.apollographql.apollo.cache.normalized.internal

/**
 * Simple stack data structure which accepts null elements. Backed by list.
 * @param <E>
</E> */
class SimpleStack<E> {
  private val backing: MutableList<E>

  constructor() {
    backing = ArrayList()
  }

  constructor(initialSize: Int) {
    backing = ArrayList(initialSize)
  }

  fun push(element: E) {
    backing.add(element)
  }

  fun pop(): E {
    check(!isEmpty) { "Stack is empty." }
    return backing.removeAt(backing.size - 1)
  }

  val isEmpty: Boolean
    get() = backing.isEmpty()
}