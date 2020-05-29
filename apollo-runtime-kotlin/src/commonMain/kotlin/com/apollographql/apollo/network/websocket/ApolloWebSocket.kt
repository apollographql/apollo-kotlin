package com.apollographql.apollo.network.websocket

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import okio.ByteString

@ExperimentalCoroutinesApi
expect class ApolloWebSocketFactory constructor(
    serverUrl: String,
    headers: Map<String, String>
) {
  suspend fun open(): ApolloWebSocketConnection
}

@ExperimentalCoroutinesApi
expect class ApolloWebSocketConnection : ReceiveChannel<ByteString> {

  fun send(data: ByteString)

  fun close()
}
