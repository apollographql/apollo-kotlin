package com.apollographql.apollo3.network.ws

class KtorWebSocketEngine : WebSocketEngine {
  override suspend fun open(url: String, headers: Map<String, String>): WebSocketConnection {
    TODO("WebSocket are not supported on JS")
  }
}

@Suppress("FunctionName")
actual fun WebSocketEngine(): WebSocketEngine = KtorWebSocketEngine()