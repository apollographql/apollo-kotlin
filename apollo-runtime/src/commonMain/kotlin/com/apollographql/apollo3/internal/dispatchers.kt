package com.apollographql.apollo3.internal

import kotlinx.coroutines.CoroutineDispatcher
import okio.Closeable

internal expect fun defaultDispatcher(requested: CoroutineDispatcher?): CoroutineDispatcher

/**
 * A coroutine dispatcher backed by a single thread that can continue to run in the background
 * until it is closed. Typically, to handle a WebSocket connection or batched HTTP queries.
 */
internal expect class CloseableSingleThreadDispatcher() : Closeable {
  val coroutineDispatcher: CoroutineDispatcher
}
