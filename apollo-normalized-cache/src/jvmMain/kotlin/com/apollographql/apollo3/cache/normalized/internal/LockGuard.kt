package com.apollographql.apollo3.cache.normalized.internal

import java.util.concurrent.locks.ReentrantReadWriteLock

actual class Guard<R: Any> actual constructor(name: String, producer: () -> R) {
  private val lock = ReentrantReadWriteLock()
  private val resource = producer()

  actual fun <T> access(block: (R) -> T): T {
    return lock.access {
      block(resource)
    }
  }

  actual fun writeAndForget(block: (R) -> Unit) {
    lock.write {
      block(resource)
    }
  }
}