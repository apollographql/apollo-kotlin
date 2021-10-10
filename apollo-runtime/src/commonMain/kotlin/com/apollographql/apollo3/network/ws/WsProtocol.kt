package com.apollographql.apollo3.network.ws

import com.apollographql.apollo3.api.AnyAdapter
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.internal.json.buildJsonByteString
import com.apollographql.apollo3.api.internal.json.buildJsonString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okio.Buffer

/**
 * A [WsProtocol] is responsible for handling the details of the WebSocket protocol.
 *
 * Implementations must implement [WsProtocol.readWebsocket], [WsProtocol.startOperation], [WsProtocol.stopOperation]
 * Additionally, implementation can use the provided [scope] to implement keep alive or other long running processes
 *
 * [WsProtocol.readWebsocket], [WsProtocol.startOperation], [WsProtocol.stopOperation] and [scope] all share the same
 * thread and rely on [webSocketConnection] to do the operations async
 *
 * @param webSocketConnection the connection
 * @param listener a listener
 * @param scope a [CoroutineScope] bound to this websocket
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
     * An error was received in response to an operationStart
     */
    fun operationError(id: String, payload: Map<String, Any?>?)

    /**
     * An operation is complete
     */
    fun operationComplete(id: String)

    /**
     * a general network error was received.
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
  abstract fun <D: Operation.Data> startOperation(request: ApolloRequest<D>)

  /**
   * Stops the given operation
   */
  abstract fun <D: Operation.Data> stopOperation(request: ApolloRequest<D>)

  protected fun Map<String, Any?>.toByteString() = buildJsonByteString {
    AnyAdapter.toJson(this, this@toByteString)
  }

  protected fun Map<String, Any?>.toUtf8() = buildJsonString {
    AnyAdapter.toJson(this, this@toUtf8)
  }

  protected fun String.toMessageMap() = AnyAdapter.fromJson(BufferedSourceJsonReader(Buffer().writeUtf8(this))) as Map<String, Any?>

  protected fun sendMessageMapBinary(messageMap: Map<String, Any?>) {
    webSocketConnection.send(messageMap.toByteString())
  }
  protected fun sendMessageMapText(messageMap: Map<String, Any?>) {
    webSocketConnection.send(messageMap.toUtf8())
  }

  protected suspend fun receiveMessageMap() = webSocketConnection.receive().toMessageMap()

  open fun run(scope: CoroutineScope) {
    scope.launch {
      try {
        while(true) {
          handleServerMessage(receiveMessageMap())
        }
      } catch (e: Exception) {
        listener.networkError(e)
      }
    }
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
    ): WsProtocol
  }
}

