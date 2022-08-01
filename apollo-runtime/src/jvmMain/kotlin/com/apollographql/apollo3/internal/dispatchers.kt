package com.apollographql.apollo3.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import okio.Closeable
import java.util.concurrent.Executors

internal actual fun defaultDispatcher(requested: CoroutineDispatcher?): CoroutineDispatcher {
  return requested ?: Dispatchers.IO
}

internal actual class CloseableSingleThreadDispatcher actual constructor() : Closeable {
  private var closed = false
  private val _dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

  actual val coroutineDispatcher: CoroutineDispatcher
    get() = _dispatcher

  override fun close() {
    if (!closed) {
      _dispatcher.close()
      closed = true
    }
  }
}
