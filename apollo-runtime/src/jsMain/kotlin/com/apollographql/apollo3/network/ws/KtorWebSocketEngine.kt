package com.apollographql.apollo3.network.ws

actual class DefaultWebSocketEngine : WebSocketEngine {
  override suspend fun open(url: String, headers: Map<String, String>): WebSocketConnection {
    TODO("WebSocket are not supported on JS")
  }
}

