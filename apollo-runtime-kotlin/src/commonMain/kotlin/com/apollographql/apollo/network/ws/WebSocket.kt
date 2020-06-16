package com.apollographql.apollo.network.ws

import kotlinx.coroutines.channels.ReceiveChannel
import okio.ByteString

interface WebSocketFactory {
  suspend fun open(headers: Map<String, String> = emptyMap()): WebSocketConnection
}

interface WebSocketConnection : ReceiveChannel<ByteString> {

  fun send(data: ByteString)

  fun close()
}

expect class ApolloWebSocketFactory constructor(
    serverUrl: String,
    headers: Map<String, String> = emptyMap()
) : WebSocketFactory
