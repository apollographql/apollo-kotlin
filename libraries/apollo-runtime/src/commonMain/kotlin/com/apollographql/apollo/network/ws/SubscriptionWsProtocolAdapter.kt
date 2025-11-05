@file:Suppress("DEPRECATION")

package com.apollographql.apollo.network.ws

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.Operation

@Deprecated("The websocket implementation has moved to 'com.apollographql.apollo.network.websocket'. See https://go.apollo.dev/ak-v5-websockets for more details.")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
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