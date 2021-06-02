package com.apollographql.apollo3.cache.normalized.internal

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

actual class Guard<R: Any> actual constructor(name: String, producer: () -> R) {
  private val lock = ReentrantReadWriteLock()
  private val resource = producer()

  actual suspend fun <T> access(block: (R) -> T): T {
    return blockingAccess(block)
  }

  actual fun writeAndForget(block: (R) -> Unit) {
    lock.write {
      block(resource)
    }
  }

  actual fun dispose() {
  }

  actual fun <T> blockingAccess(block: (R) -> T): T {
    return lock.read {
      block(resource)
    }
  }
}