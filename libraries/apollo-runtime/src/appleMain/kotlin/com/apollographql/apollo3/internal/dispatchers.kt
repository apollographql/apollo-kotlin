package com.apollographql.apollo3.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import okio.Closeable

internal actual val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default

internal actual class CloseableSingleThreadDispatcher actual constructor() : Closeable {
  private var closed = false

  @OptIn(ExperimentalCoroutinesApi::class)
  private val _dispatcher = newSingleThreadContext("Apollo Background Dispatcher")

  actual val coroutineDispatcher: CoroutineDispatcher
    get() = _dispatcher

  override fun close() {
    if (!closed) {
      _dispatcher.close()
      closed = true
    }
  }
}
