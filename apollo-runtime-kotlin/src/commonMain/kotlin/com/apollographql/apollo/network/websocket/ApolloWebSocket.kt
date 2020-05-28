package com.apollographql.apollo.network.websocket

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import okio.ByteString

@ExperimentalCoroutinesApi
expect class ApolloWebSocketFactory constructor(
    serverUrl: String,
    headers: Map<String, String>
) {
  fun open(): ApolloWebSocketConnection
}

@ExperimentalCoroutinesApi
expect class ApolloWebSocketConnection : ReceiveChannel<ApolloWebSocketConnection.Event> {

  fun send(data: ByteString)

  fun close(code: Int, reason: String?)

  sealed class Event {

    class Open : Event

    class Message : Event {
      val data: ByteString
    }
  }
}
