package com.apollographql.apollo.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import okio.Closeable

internal actual class CloseableSingleThreadDispatcher actual constructor() : Closeable {
  private var closed = false

  @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
  private val _dispatcher = newSingleThreadContext("Apollo Background Dispatcher")

  actual val coroutineDispatcher: CoroutineDispatcher
    get() = _dispatcher

  actual override fun close() {
    if (!closed) {
      _dispatcher.close()
      closed = true
    }
  }
}
