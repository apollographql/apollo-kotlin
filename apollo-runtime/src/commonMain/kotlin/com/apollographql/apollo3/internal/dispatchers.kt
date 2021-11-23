package com.apollographql.apollo3.internal

import kotlinx.coroutines.CoroutineDispatcher

internal expect fun defaultDispatcher(requested: CoroutineDispatcher?): CoroutineDispatcher

/**
 * A mutex implementation for code that runs in the defaultDispatcher
 *
 * On native, this is a no-op because the defaultDispatcher is always the main loop
 */
internal expect class DefaultMutex() : Mutex

/**
 * A coroutine dispatcher that can continue to run in the background. Typically,
 * to handle a WebSocket connection or batched HTTP queries
 *
 * On the JVM, it uses a background thread
 * On native, it uses the main thread
 */
internal expect class BackgroundDispatcher() {
  val coroutineDispatcher: CoroutineDispatcher
  fun dispose()
}
