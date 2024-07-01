package com.apollographql.apollo.network.ws

import com.apollographql.apollo.api.http.HttpHeader
import okio.ByteString

/**
 * The low-level WebSocket API. Implement this interface to customize how WebSockets are handled
 */
interface WebSocketEngine {
  /**
   * Open the websocket. Suspends until the handshake is done
   */
  suspend fun open(
      url: String,
      headers: List<HttpHeader> = emptyList(),
  ): WebSocketConnection
}

interface WebSocketConnection {
  /**
   * Suspends until a message is available and return it. If the message was binary, it is converted to a String
   *
   * @throws ApolloNetworkException if a network exception happened either when reading or sending messages
   * See also [send]
   */
  suspend fun receive(): String

  /**
   * Sends a binary message asynchronously.
   *
   * There is no flow control. If the application is sending messages too fast, the connection should be closed and
   * an error should be sent in a subsequent [receive] call.
   * See https://github.com/square/okhttp/issues/3848
   */
  fun send(data: ByteString)
  /**
   * Sends a text message asynchronously.
   *
   * There is no flow control. If the application is sending messages too fast, the connection should be closed and
   * an error should be sent in a subsequent [receive] call.
   * See https://github.com/square/okhttp/issues/3848
   */
  fun send(string: String)

  /**
   * closes the websocket gracefully and asynchronously
   */
  fun close()
}

expect class DefaultWebSocketEngine() : WebSocketEngine{
  override suspend fun open(url: String, headers: List<HttpHeader>): WebSocketConnection
}

const val CLOSE_NORMAL = 1000
const val CLOSE_GOING_AWAY = 1001
