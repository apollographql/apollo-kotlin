package com.apollographql.ijplugin.normalizedcache

class History<T> {
  private var history = mutableListOf<T>()
  private var pointer = -1

  fun current(): T? {
    return if (pointer >= 0) {
      history[pointer]
    } else {
      null
    }
  }

  fun push(element: T) {
    if (current() == element) return
    history = history.subList(0, pointer + 1)
    history.add(element)
    pointer = history.lastIndex
  }

  fun back(): T? {
    if (pointer > 0) {
      pointer--
    }
    return current()
  }

  fun forward(): T? {
    if (pointer < history.lastIndex) {
      pointer++
    }
    return current()
  }

  fun canGoBack(): Boolean {
    return pointer > 0
  }

  fun canGoForward(): Boolean {
    return pointer < history.lastIndex
  }
}
