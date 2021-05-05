package com.apollographql.apollo3.network.ws

import kotlinx.coroutines.channels.ReceiveChannel
import okio.ByteString

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
   * Should not throw
   */
  fun close()
}

expect class DefaultWebSocketEngine() : WebSocketEngine

const val CLOSE_NORMAL = 1000
const val CLOSE_GOING_AWAY = 1001
