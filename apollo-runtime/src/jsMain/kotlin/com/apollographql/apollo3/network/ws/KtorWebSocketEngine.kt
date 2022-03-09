package com.apollographql.apollo3.network.ws

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.webSocketSession
import io.ktor.client.request.headers
import io.ktor.http.Url
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.ByteString

actual class DefaultWebSocketEngine(private val ktorClient: HttpClient) : WebSocketEngine {
  actual constructor() : this(ktorClient = HttpClient(Js) { install(WebSockets) })

  override suspend fun open(
      url: String,
      headers: Map<String, String>,
  ): WebSocketConnection = open(Url(url), headers)

  private suspend fun open(url:Url, headers: Map<String, String>): WebSocketConnection {
    val socketSession = ktorClient.webSocketSession(host = url.host, port = url.port, path = url.encodedPath) {
      headers {
        headers.forEach {
          append(it.key, it.value)
        }
      }
    }

    return object : WebSocketConnection {
      override suspend fun receive(): String {
        return socketSession.incoming.receive().data.decodeToString()
      }

      override fun send(data: ByteString) {
        socketSession.outgoing.trySend(Frame.Binary(true, data.toByteArray()))
      }

      override fun send(string: String) {
        socketSession.outgoing.trySend(Frame.Text(string))
      }

      override fun close() {
        CoroutineScope(Dispatchers.Unconfined).launch {
          socketSession.close(CloseReason(CloseReason.Codes.NORMAL, ""))
        }
      }
    }
  }
}

