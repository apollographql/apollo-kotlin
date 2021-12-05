package com.apollographql.apollo3.network.ws

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.exception.ApolloNetworkException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeout

/**
 * A [WsProtocol] for https://github.com/apollographql/subscriptions-transport-ws/blob/master/PROTOCOL.md
 */
class SubscriptionWsProtocol(
    webSocketConnection: WebSocketConnection,
    listener: Listener,
    private val connectionAcknowledgeTimeoutMs: Long = 10_000,
    private val connectionPayload: suspend () -> Map<String, Any?>? = { null },
) : WsProtocol(webSocketConnection, listener) {

  override suspend fun connectionInit() {
    val message = mutableMapOf<String, Any?>(
        "type" to "connection_init",
    )

    val payload = connectionPayload()
    if (payload != null) {
      message.put("payload", payload)
    }

    sendMessageMapBinary(message)

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
    sendMessageMapBinary(
        mapOf(
            "type" to "start",
            "id" to request.requestUuid.toString(),
            "payload" to DefaultHttpRequestComposer.composePayload(request)
        )
    )
  }

  override fun <D : Operation.Data> stopOperation(request: ApolloRequest<D>) {
    sendMessageMapBinary(
        mapOf(
            "type" to "stop",
            "id" to request.requestUuid.toString(),
        )
    )
  }

  class Factory(
      private val connectionAcknowledgeTimeoutMs: Long = 10_000,
      private val connectionPayload: suspend () -> Map<String, Any?>? = { null },
  ) : WsProtocol.Factory {
    override val name: String
      get() = "graphql-ws"

    override fun create(
        webSocketConnection: WebSocketConnection,
        listener: Listener,
        scope: CoroutineScope
    ): WsProtocol {
      return SubscriptionWsProtocol(
          connectionPayload = connectionPayload,
          connectionAcknowledgeTimeoutMs = connectionAcknowledgeTimeoutMs,
          webSocketConnection = webSocketConnection,
          listener = listener,
      )
    }
  }
}