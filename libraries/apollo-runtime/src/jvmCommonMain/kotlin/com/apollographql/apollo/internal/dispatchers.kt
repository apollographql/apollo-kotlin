package com.apollographql.apollo.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import okio.Closeable
import java.util.concurrent.Executors

internal actual class CloseableSingleThreadDispatcher actual constructor() : Closeable {
  private var closed = false
  private val _dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

  actual val coroutineDispatcher: CoroutineDispatcher
    get() = _dispatcher

  actual override fun close() {
    if (!closed) {
      _dispatcher.close()
      closed = true
    }
  }
}

