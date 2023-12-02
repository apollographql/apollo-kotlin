package com.apollographql.apollo3.network.ws

import com.apollographql.apollo3.api.http.HttpHeader

actual class DefaultWebSocketEngine actual constructor() : WebSocketEngine {
  actual override suspend fun open(
      url: String,
      headers: List<HttpHeader>,
  ): WebSocketConnection {
    TODO("Not yet implemented")
  }
}