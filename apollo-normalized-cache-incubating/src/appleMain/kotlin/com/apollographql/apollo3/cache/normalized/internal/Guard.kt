package com.apollographql.apollo3.cache.normalized.internal

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal actual class Guard<R : Any> actual constructor(name: String, producer: () -> R) {
  private val mutex = Mutex()
  private val resource = producer()

  actual suspend fun <T> readAccess(block: (R) -> T): T {
    return mutex.withLock {
      block(resource)
    }
  }

  actual suspend fun <T> writeAccess(block: (R) -> T): T {
    return mutex.withLock {
      block(resource)
    }
  }

  actual fun writeAndForget(block: (R) -> Unit) = runBlocking {
    mutex.withLock {
      block(resource)
    }
  }

  actual fun dispose() {
  }
}
