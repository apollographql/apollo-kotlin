package test.fake_subscription_engine

import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.network.ws.WebSocketConnection
import com.apollographql.apollo3.network.ws.WebSocketEngine
import okio.ByteString

class FakeWebSocketEngine(val onReceive: suspend () -> String, val onSend: (String) -> Unit) : WebSocketEngine {
  override suspend fun open(url: String, headers: List<HttpHeader>): WebSocketConnection {
    return FakeWebSocketConnection(onReceive, onSend)
  }
}

class FakeWebSocketConnection(val onReceive: suspend () -> String, val onSend: (String) -> Unit) : WebSocketConnection {
  override suspend fun receive(): String {
    return onReceive()
  }

  override fun send(data: ByteString) {
    onSend(data.utf8())
  }

  override fun send(string: String) {
    onSend(string)
  }

  override fun close() {
  }
}