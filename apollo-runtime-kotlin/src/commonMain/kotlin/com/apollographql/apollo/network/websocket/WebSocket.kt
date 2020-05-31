package com.apollographql.apollo.network.websocket

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import okio.ByteString

@ExperimentalCoroutinesApi
expect class WebSocketFactory constructor(
    serverUrl: String,
    headers: Map<String, String> = emptyMap()
) {
  suspend fun open(headers: Map<String, String> = emptyMap()): WebSocketConnection
}

@ExperimentalCoroutinesApi
expect class WebSocketConnection : ReceiveChannel<ByteString> {

  fun send(data: ByteString)

  fun close()
}
