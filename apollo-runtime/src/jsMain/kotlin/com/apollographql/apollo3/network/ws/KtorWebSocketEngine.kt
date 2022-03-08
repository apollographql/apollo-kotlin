package com.apollographql.apollo3.network.ws

import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.webSocketSession
import io.ktor.client.request.headers
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.ByteString

actual class DefaultWebSocketEngine(
    private val ktorClient: HttpClient,
) : WebSocketEngine {
  private val coroutineScope = CoroutineScope(Dispatchers.Main)
  actual constructor(): this(ktorClient = HttpClient())

  override suspend fun open(
      url: String,
      headers: Map<String, String>,
  ): WebSocketConnection {
    val session = ktorClient.webSocketSession {
      headers {
        headers.forEach { (key, value) ->
          append(key, value)
        }
      }
    }

    return object : WebSocketConnection {
      override suspend fun receive(): String {
        return session.incoming.receive().data.decodeToString()
      }

      override fun send(data: ByteString) {
        session.outgoing.trySend(Frame.Binary(false, data.toByteArray()))
      }

      override fun send(string: String) {
          session.outgoing.trySend(Frame.Text(string))
      }

      override fun close() {
        coroutineScope.launch {
          session.close(CloseReason(CloseReason.Codes.NORMAL, ""))
        }
      }
    }
  }
}

