package com.apollographql.apollo3.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.newSingleThreadContext

private val defaultDispatcher = newFixedThreadPoolContext(nThreads = 32, name = "Apollo Default Dispatcher")

internal actual fun defaultDispatcher(requested: CoroutineDispatcher?): CoroutineDispatcher {
  return requested ?: defaultDispatcher
}

internal actual class BackgroundDispatcher actual constructor() {
  private var disposed = false

  @OptIn(ExperimentalCoroutinesApi::class)
  private val _dispatcher = newSingleThreadContext("Apollo Background Dispatcher")

  actual val coroutineDispatcher: CoroutineDispatcher
    get() = _dispatcher

  actual fun dispose() {
    if (!disposed) {
      _dispatcher.close()
      disposed = true
    }
  }
}
