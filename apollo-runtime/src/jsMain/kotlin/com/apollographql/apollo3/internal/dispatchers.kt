package com.apollographql.apollo3.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual fun defaultDispatcher(requested: CoroutineDispatcher?): CoroutineDispatcher {
  return requested ?: Dispatchers.Default
}

// We can't use threads in JS, so just fallback to defaultDispatcher()
internal actual class BackgroundDispatcher {
  actual val coroutineDispatcher: CoroutineDispatcher = defaultDispatcher(null)

  actual fun dispose() {
  }
}
