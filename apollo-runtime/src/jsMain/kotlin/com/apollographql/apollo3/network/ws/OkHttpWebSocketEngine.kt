package com.apollographql.apollo3.network.ws

import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
actual class DefaultWebSocketEngine : WebSocketEngine {
  override suspend fun open(url: String, headers: Map<String, String>): WebSocketConnection {
    TODO()
  }
}