package com.apollographql.apollo3.network.ws

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.AnyAdapter
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.json.buildJsonByteString
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.api.json.writeAny
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import okio.Buffer

/**
 * A [WsProtocol] is responsible for handling the details of the WebSocket protocol.
 *
 * Implementations must implement [WsProtocol.handleServerMessage], [WsProtocol.startOperation], [WsProtocol.stopOperation]
 *
 * [WsProtocol.handleServerMessage], [WsProtocol.startOperation], [WsProtocol.stopOperation]  all share the same
 * thread and rely on [webSocketConnection] to do the operations async
 *
 * @param webSocketConnection the connection
 * @param listener a listener
 */
abstract class WsProtocol(
    protected val webSocketConnection: WebSocketConnection,
    protected val listener: Listener,
) {

  interface Listener {
    /**
     * A response was received. payload might contain "errors"
     * For subscriptions, several responses might be received.
     */
    fun operationResponse(id: String, payload: Map<String, Any?>)

    /**
     * An error was received in relation to an operation
     */
    fun operationError(id: String, payload: Map<String, Any?>?)

    /**
     * An operation is complete
     */
    fun operationComplete(id: String)

    /**
     * A general error was received
     * A general error is a protocol error that doesn't have an operation id
     * If you have an operation id, use [operationError] instead
     */
    fun generalError(payload: Map<String, Any?>?)

    /**
     * A network error occurred
     * A network error is terminal
     */
    fun networkError(cause: Throwable)
  }

  /**
   * Initializes the connection and suspends until the server acknowledges it.
   *
   * @throws Exception
   */
  abstract suspend fun connectionInit()

  /**
   * Handles a server message and notifies [listener] appropriately
   */
  abstract fun handleServerMessage(messageMap: Map<String, Any?>)

  /**
   * Starts the given operation
   */
  abstract fun <D : Operation.Data> startOperation(request: ApolloRequest<D>)

  /**
   * Stops the given operation
   */
  abstract fun <D : Operation.Data> stopOperation(request: ApolloRequest<D>)

  @OptIn(ApolloInternal::class)
  protected fun Map<String, Any?>.toByteString() = buildJsonByteString {
    writeAny(this@toByteString)
  }

  @OptIn(ApolloInternal::class)
  protected fun Map<String, Any?>.toUtf8() = buildJsonString {
    writeAny(this@toUtf8)
  }

  @Suppress("UNCHECKED_CAST")
  protected fun String.toMessageMap() = AnyAdapter.fromJson(
      BufferedSourceJsonReader(Buffer().writeUtf8(this)),
      CustomScalarAdapters.Empty
  ) as Map<String, Any?>

  protected fun sendMessageMapBinary(messageMap: Map<String, Any?>) {
    webSocketConnection.send(messageMap.toByteString())
  }

  protected fun sendMessageMapText(messageMap: Map<String, Any?>) {
    webSocketConnection.send(messageMap.toUtf8())
  }

  protected fun sendMessageMap(messageMap: Map<String, Any?>, frameType: WsFrameType) {
    when (frameType) {
      WsFrameType.Text -> sendMessageMapText(messageMap)
      WsFrameType.Binary -> sendMessageMapBinary(messageMap)
    }
  }

  protected suspend fun receiveMessageMap() = webSocketConnection.receive().toMessageMap()

  /**
   * Read the WebSocket
   */
  open suspend fun run() {
    try {
      while (true) {
        handleServerMessage(receiveMessageMap())
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      listener.networkError(e)
    }
  }

  /**
   * Closes the connection gracefully.
   * It is expected that a future call to [WsProtocol.Listener.networkError] is made
   */
  open fun close() {
    webSocketConnection.close()
  }

  interface Factory {
    /**
     * The name of the protocol as in the Sec-WebSocket-Protocol header
     *
     * Example: "graphql-transport-ws" or "graphql-ws"
     */
    val name: String

    /**
     * Create a [WsProtocol]
     */
    fun create(
        webSocketConnection: WebSocketConnection,
        listener: Listener,
        scope: CoroutineScope,
    ): WsProtocol
  }
}

enum class WsFrameType {
  Text,
  Binary
}
