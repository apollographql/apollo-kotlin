package com.apollographql.apollo.network.websocket

import kotlinx.coroutines.channels.ReceiveChannel
import okio.ByteString

expect class ApolloWebSocketFactory constructor(
    serverUrl: String,
    headers: Map<String, String>
) {
  fun open(): ApolloWebSocketConnection
}

expect class ApolloWebSocketConnection : ReceiveChannel<ApolloWebSocketConnection.Event> {

  fun send(data: ByteString)

  fun close(code: Int, reason: String?)

  sealed class Event {

    class OnOpen : Event

    class OnClosed : Event

    class OnMessage : Event {
      val data: ByteString
    }
  }
}
