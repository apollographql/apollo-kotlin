package com.apollographql.apollo3.cache.normalized.internal

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal actual class Guard<R: Any> actual constructor(name: String, producer: () -> R) {
  private val lock = ReentrantReadWriteLock()
  private val resource = producer()

  actual suspend fun <T> readAccess(block: (R) -> T): T {
    return lock.read {
      block(resource)
    }
  }

  actual suspend fun <T> writeAccess(block: (R) -> T): T {
    return lock.write {
      block(resource)
    }
  }

  actual fun writeAndForget(block: (R) -> Unit) {
    lock.write {
      block(resource)
    }
  }

  actual fun dispose() {
  }
}