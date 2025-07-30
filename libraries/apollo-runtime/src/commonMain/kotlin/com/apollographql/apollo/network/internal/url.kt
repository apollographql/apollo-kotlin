package com.apollographql.apollo.network.internal

/**
 * Converts an url to a WebSocket url as expected by the browser WebSocket API.
 */
internal fun String.toWebSocketUrl(): String {
  return when {
    startsWith("http://") -> "ws://${substring(7)}"
    startsWith("https://") -> "wss://${substring(8)}"
    else -> this
  }
}