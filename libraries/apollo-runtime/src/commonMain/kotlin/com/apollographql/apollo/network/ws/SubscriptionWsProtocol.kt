package com.apollographql.apollo.network.ws

import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo.exception.ApolloNetworkException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeout
import kotlin.jvm.JvmOverloads

/**
 * A [WsProtocol] for https://github.com/apollographql/subscriptions-transport-ws/blob/master/PROTOCOL.md
 *
 * Note: This protocol is no longer actively maintained, and [GraphQLWsProtocol] should be favored instead.
 */
class SubscriptionWsProtocol
@JvmOverloads
constructor(
    webSocketConnection: WebSocketConnection,
    listener: Listener,
    private val connectionAcknowledgeTimeoutMs: Long = 10_000,
    private val connectionPayload: suspend () -> Map<String, Any?>? = { null },
    private val frameType: WsFrameType = WsFrameType.Text,
) : WsProtocol(webSocketConnection, listener) {

  override suspend fun connectionInit() {
    val message = mutableMapOf<String, Any?>(
        "type" to "connection_init",
    )

    val payload = connectionPayload()
    if (payload != null) {
      message.put("payload", payload)
    }

    sendMessageMap(message, frameType)

    withTimeout(connectionAcknowledgeTimeoutMs) {
      val map = receiveMessageMap()
      when (val type = map["type"]) {
        "connection_ack" -> return@withTimeout
        "connection_error" -> throw ApolloNetworkException("Connection error:\n$map")
        else -> println("unknown message while waiting for connection_ack: '$type")
      }
    }
  }

  override fun handleServerMessage(messageMap: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    when (messageMap["type"]) {
      "data" -> listener.operationResponse(messageMap["id"] as String, messageMap["payload"] as Map<String, Any?>)
      "error" -> {
        val id = messageMap["id"]
        if (id is String) {
          listener.operationError(id, messageMap["payload"] as Map<String, Any?>?)
        } else {
          listener.generalError(messageMap["payload"] as Map<String, Any?>?)
        }
      }

      "complete" -> listener.operationComplete(messageMap["id"] as String)
      else -> Unit // unknown message...
    }
  }

  override fun <D : Operation.Data> startOperation(request: ApolloRequest<D>) {
    sendMessageMap(
        mapOf(
            "type" to "start",
            "id" to request.requestUuid.toString(),
            "payload" to DefaultHttpRequestComposer.composePayload(request)
        ),
        frameType
    )
  }

  override fun <D : Operation.Data> stopOperation(request: ApolloRequest<D>) {
    sendMessageMap(
        mapOf(
            "type" to "stop",
            "id" to request.requestUuid.toString(),
        ),
        frameType
    )
  }

  /**
   * A factory for [SubscriptionWsProtocol].
   *
   * @param connectionAcknowledgeTimeoutMs the timeout for receiving the "connection_ack" message, in milliseconds
   * @param connectionPayload a map of additional parameters to send in the "connection_init" message
   * @param frameType the type of the websocket frames to use. Default value: [WsFrameType.Text]
   */
  class Factory
  @JvmOverloads
  constructor(
      private val connectionAcknowledgeTimeoutMs: Long = 10_000,
      private val connectionPayload: suspend () -> Map<String, Any?>? = { null },
      private val frameType: WsFrameType = WsFrameType.Text,
  ) : WsProtocol.Factory {
    override val name: String
      get() = "graphql-ws"

    override fun create(
        webSocketConnection: WebSocketConnection,
        listener: Listener,
        scope: CoroutineScope,
    ): WsProtocol {
      return SubscriptionWsProtocol(
          connectionPayload = connectionPayload,
          connectionAcknowledgeTimeoutMs = connectionAcknowledgeTimeoutMs,
          webSocketConnection = webSocketConnection,
          listener = listener,
          frameType = frameType,
      )
    }
  }
}