package com.apollographql.apollo3.network.ws

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_2_2
import com.apollographql.apollo3.api.http.HttpHeader
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

  /**
   * Open the websocket. Suspends until the handshake is done
   */
  @Deprecated(
      "Use open(String, List<HttpHeader>) instead.",
      ReplaceWith(
          "open(url, headers.map { HttpHeader(it.key, it.value })",
          "com.apollographql.apollo3.api.http.HttpHeader"
      )
  )
  @ApolloDeprecatedSince(v3_2_2)
  suspend fun open(
      url: String,
      headers: Map<String, String> = emptyMap(),
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

expect class DefaultWebSocketEngine() : WebSocketEngine

const val CLOSE_NORMAL = 1000
const val CLOSE_GOING_AWAY = 1001
