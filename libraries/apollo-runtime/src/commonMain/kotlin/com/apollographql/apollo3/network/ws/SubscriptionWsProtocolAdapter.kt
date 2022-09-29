package com.apollographql.apollo3.network.ws

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Operation

open class SubscriptionWsProtocolAdapter(webSocketConnection: WebSocketConnection, listener: Listener): WsProtocol(webSocketConnection, listener) {
  private val delegate = SubscriptionWsProtocol(webSocketConnection, listener)

  override suspend fun connectionInit() {
    delegate.connectionInit()
  }

  override fun handleServerMessage(messageMap: Map<String, Any?>) {
    delegate.handleServerMessage(messageMap)
  }

  override fun <D : Operation.Data> startOperation(request: ApolloRequest<D>) {
    delegate.startOperation(request)
  }

  override fun <D : Operation.Data> stopOperation(request: ApolloRequest<D>) {
    delegate.stopOperation(request)
  }
}