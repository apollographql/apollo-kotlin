package com.apollographql.apollo3.network.websocket

import com.apollographql.apollo3.api.http.HttpHeader

interface WebSocketEngine {
  /**
   * Creates a new [WebSocket].
   *
   * The [WebSocket] starts in unconnected state where no calls to [listener] will be made.
   * Call [WebSocket.connect] when ready to start the network connection.
   *
   * The [WebSocket] will be garbage collected when not used anymore. To trigger
   * that from the client side, call [WebSocket.close]. You don't need to call [WebSocket.close] if:
   * - [WebSocket.connect] was never called
   * - [WebSocketListener.onError] has been called
   * - [WebSocketListener.onClosed] has been called
   *
   * @param url: an url starting with one of:
   * - ws://
   * - wss://
   * - http://
   * - https://
   *
   * If the underlying engine requires a ws or wss, http and https are replaced by ws and wss respectively
   */
  fun newWebSocket(
      url: String,
      headers: List<HttpHeader> = emptyList(),
      listener: WebSocketListener
  ): WebSocket
}

interface WebSocketListener {
  /**
   * The HTTP 101 Switching Protocols response has been received and is valid.
   */
  fun onOpen()

  /**
   * A text message has been received.
   */
  fun onMessage(text: String)


  /**
   * A data message has been received.
   */
  fun onMessage(data: ByteArray)

  /**
   * An error happened, no more calls to the listener are made.
   */
  fun onError(throwable: Throwable)

  /**
   * The server sent a close frame, no more calls to the listener are made.
   */
  fun onClosed(code: Int?, reason: String?)
}

interface WebSocket {
  /**
   * Connects to the peer and starts reading the socket.
   *
   * No calls to the listener are made before [connect]
   */
  fun connect()


  /**
   * Sends a binary message asynchronously.
   *
   * There is no flow control. If the application is sending messages too fast, the connection is closed.
   */
  fun send(data: ByteArray)

  /**
   * Sends a text message asynchronously.
   *
   * There is no flow control. If the application is sending messages too fast, the connection is closed.
   */
  fun send(text: String)

  /**
   * Closes the websocket gracefully and asynchronously.
   *
   * After this call, no more calls to the listener are made
   *
   * Note: there is no API to retrieve the server close code because Apple cancelWithCloseCode calls the
   * URLSession delegate with the same (client) code.
   */
  fun close(code: Int, reason: String)
}

expect fun WebSocketEngine() : WebSocketEngine

internal const val CLOSE_NORMAL = 1000
internal const val CLOSE_GOING_AWAY = 1001
