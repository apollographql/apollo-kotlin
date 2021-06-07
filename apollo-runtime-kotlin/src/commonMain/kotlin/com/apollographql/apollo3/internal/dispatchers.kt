package com.apollographql.apollo3.internal

import kotlinx.coroutines.CoroutineDispatcher

expect fun defaultDispatcher(requested: CoroutineDispatcher?): CoroutineDispatcher

expect class WebSocketDispatcher() {
  val coroutineDispatcher: CoroutineDispatcher
  fun dispose()
}
