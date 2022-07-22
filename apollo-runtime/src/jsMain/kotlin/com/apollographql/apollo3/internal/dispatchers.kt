package com.apollographql.apollo3.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okio.Closeable

internal actual fun defaultDispatcher(requested: CoroutineDispatcher?): CoroutineDispatcher {
  return requested ?: Dispatchers.Default
}

// We can't use threads in JS, so just fallback to defaultDispatcher()
internal actual class CloseableSingleThreadDispatcher : Closeable {
  actual val coroutineDispatcher: CoroutineDispatcher = defaultDispatcher(null)

  override fun close() {
  }
}
