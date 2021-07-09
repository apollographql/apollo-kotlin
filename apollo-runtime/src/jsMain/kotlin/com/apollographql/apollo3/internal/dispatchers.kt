package com.apollographql.apollo3.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual fun defaultDispatcher(requested: CoroutineDispatcher?): CoroutineDispatcher {
  return requested ?: Dispatchers.Default
}

// We can't use threads in JS, so just fallback to defaultDispatcher()
actual class BackgroundDispatcher {
  actual val coroutineDispatcher: CoroutineDispatcher = defaultDispatcher(null)

  actual fun dispose() {
  }
}

actual class DefaultMutex: Mutex {
  private val lock = Unit

  override fun <T> lock(block: () -> T): T {
    synchronized(lock) {
      return block()
    }
  }
}