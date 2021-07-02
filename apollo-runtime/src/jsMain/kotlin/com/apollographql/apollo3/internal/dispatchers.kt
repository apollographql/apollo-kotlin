package com.apollographql.apollo3.internal

import kotlinx.coroutines.CoroutineDispatcher

actual fun defaultDispatcher(requested: CoroutineDispatcher?): CoroutineDispatcher {
  TODO()
}

actual class WebSocketDispatcher {
  actual val coroutineDispatcher: CoroutineDispatcher = TODO()

  actual fun dispose() {
    TODO()
  }
}