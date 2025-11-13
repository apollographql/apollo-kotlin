@file:Suppress("DEPRECATION")
package com.apollographql.apollo.network.ws

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.api.http.HttpHeader

@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
@Deprecated("The websocket implementation has moved to 'com.apollographql.apollo.network.websocket'. See https://go.apollo.dev/ak-v5-websockets for more details.")
actual class DefaultWebSocketEngine : WebSocketEngine {

  actual override suspend fun open(
      url: String,
      headers: List<HttpHeader>,
  ): WebSocketConnection = TODO("Not yet implemented")
}