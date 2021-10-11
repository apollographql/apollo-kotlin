package com.apollographql.apollo3.network.ws

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Operation

/**
 * A [WsProtocol] is responsible for handling the details of the WebSocketProtocol
 */
interface WsProtocol {
  /**
   * Whether to send binary or text frames
   */
  val frameType: WsFrameType

  /**
   * The name of the protocol as in the Sec-WebSocket-Protocol header
   *
   * Example: "graphql-transport-ws" or "graphql-ws"
   */
  val name: String

  /**
   * The message to send to initialize a connection. This method can suspend if updating/refreshing authorization parameters is
   * required
   */
  suspend fun connectionInit(): Map<String, Any?>
  fun connectionTerminate(): Map<String, Any?>?
  fun <D: Operation.Data> operationStart(request: ApolloRequest<D>): Map<String, Any?>
  fun <D: Operation.Data> operationStop(request: ApolloRequest<D>): Map<String, Any?>

  /**
   * ping and pong messages for graphql-ws
   */
  fun ping(payload: Map<String, Any?>? = null): Map<String, Any?>?
  fun pong(payload: Map<String, Any?>? = null): Map<String, Any?>?

  /**
   * parse the given message and return one of [WsServerMessage]
   *
   * @param message the message. If the message is received as binary, it will be converted to a String. Non-text binary messages are
   * not supported.
   * @param webSocketConnection the [WebSocketConnection]. This can be used by [WsProtocol] implementations to react to a given message.
   * For an example a [WsProtocol] might implement a custom ping/pong scheme this way
   */
  fun parseMessage(message: String, webSocketConnection: WebSocketConnection): WsServerMessage
}

enum class WsFrameType {
  Text,
  Binary
}
sealed class WsServerMessage {
  object ConnectionAck : WsServerMessage()
  class ConnectionError(val payload: Map<String, Any?>?) : WsServerMessage()
  class OperationData(val id: String, val payload: Map<String, Any?>) : WsServerMessage()
  class OperationError(val id: String, val payload: Map<String, Any?>) : WsServerMessage()
  class OperationComplete(val id: String): WsServerMessage()
  class Ping(val payload: Map<String, Any?>?): WsServerMessage()
  class Pong(val payload: Map<String, Any?>?): WsServerMessage()
  object KeepAlive: WsServerMessage()
  class Unknown(val map: Map<String, Any?>): WsServerMessage()
}

