package com.apollographql.apollo3.cache.normalized.internal

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal actual fun runBlockingWithMutex(mutex: Mutex, block: () -> Unit) {
  runBlocking {
    mutex.withLock {
      block()
    }
  }
}
