package com.apollographql.apollo.network.ws

import com.apollographql.apollo.api.http.HttpHeader

actual class DefaultWebSocketEngine actual constructor() : WebSocketEngine {
  actual override suspend fun open(
      url: String,
      headers: List<HttpHeader>,
  ): WebSocketConnection {
    TODO("Not yet implemented")
  }
}