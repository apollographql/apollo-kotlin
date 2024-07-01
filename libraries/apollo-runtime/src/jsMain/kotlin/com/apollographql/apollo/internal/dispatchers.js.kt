package com.apollographql.apollo.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okio.Closeable

internal actual val defaultDispatcher: CoroutineDispatcher
  get() = Dispatchers.Default

// We can't use threads in JS, so just fallback to defaultDispatcher()
internal actual class CloseableSingleThreadDispatcher : Closeable {
  actual val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default

  actual override fun close() {
  }
}
