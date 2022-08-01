package com.apollographql.apollo3.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okio.Closeable

internal actual val defaultDispatcher = Dispatchers.Default


// We can't use threads in JS, so just fallback to defaultDispatcher()
internal actual class CloseableSingleThreadDispatcher : Closeable {
  actual val coroutineDispatcher: CoroutineDispatcher = defaultDispatcher

  override fun close() {
  }
}

internal actual fun failOnNativeIfLegacyMemoryManager() {}
