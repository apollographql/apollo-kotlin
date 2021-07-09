package com.apollographql.apollo3.network.ws

import okio.ByteString

/**
 * The low-level WebSocket API. Implement this interface to customize how WebSockets are handled
 */
interface WebSocketEngine {
  /**
   * Should not throw
   */
  suspend fun open(
      url: String,
      headers: Map<String, String> = emptyMap(),
  ): WebSocketConnection
}

interface WebSocketConnection {
  /**
   * May throw ApolloNetworkException
   */
  suspend fun receive(): ByteString
  /**
   * May throw ApolloNetworkException
   */
  suspend fun send(data: ByteString)
  /**
   * May throw ApolloNetworkException
   */
  suspend fun send(string: String)

  /**
   * Should not throw
   */
  fun close()
}

expect class DefaultWebSocketEngine() : WebSocketEngine

const val CLOSE_NORMAL = 1000
const val CLOSE_GOING_AWAY = 1001
