package com.apollographql.apollo3.network.ws

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.http.HttpHeader
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.headers
import io.ktor.client.request.url
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okio.ByteString

@ApolloExperimental
class KtorWebSocketEngine(
    private val client: HttpClient,
) : WebSocketEngine {

  constructor() : this(
      HttpClient {
        install(WebSockets)
      }
  )

  override suspend fun open(
      url: String,
      headers: List<HttpHeader>,
  ): WebSocketConnection = open(Url(url), headers)

  private suspend fun open(url: Url, headers: List<HttpHeader>): WebSocketConnection {
    val newUrl = URLBuilder(url).apply {
      protocol = when (url.protocol) {
        URLProtocol.HTTPS -> URLProtocol.WSS
        URLProtocol.HTTP -> URLProtocol.WS
        URLProtocol.WS, URLProtocol.WSS -> url.protocol
        /* URLProtocol.SOCKS */else -> throw UnsupportedOperationException("SOCKS is not a supported protocol")
      }
    }.build()
    val receiveMessageChannel = Channel<String>(Channel.UNLIMITED)
    val sendFrameChannel = Channel<Frame>(Channel.UNLIMITED)
    client.webSocket(
        request = {
          headers {
            headers.forEach {
              append(it.name, it.value)
            }
          }
          url(newUrl)
        },
    ) {
      coroutineScope {
        launch {
          while (true) {
            when (val frame = incoming.receive()) {
              is Frame.Text -> {
                receiveMessageChannel.send(frame.readText())
              }
              is Frame.Binary -> {
                receiveMessageChannel.send(frame.data.decodeToString())
              }
              is Frame.Ping -> {
                send(Frame.Pong(frame.data))
              }
              is Frame.Pong -> {}
              is Frame.Close -> {
                close()
                receiveMessageChannel.close()
              }
              else -> error("unknown frame type")
            }
          }
        }
        launch {
          while (true) {
            val frame = sendFrameChannel.receive()
            send(frame)
          }
        }
      }
    }
    return object : WebSocketConnection {
      override suspend fun receive(): String {
        return receiveMessageChannel.receive()
      }

      override fun send(data: ByteString) {
        sendFrameChannel.trySend(Frame.Binary(true, data.toByteArray()))
      }

      override fun send(string: String) {
        sendFrameChannel.trySend(Frame.Text(string))
      }

      override fun close() {
        sendFrameChannel.trySend(Frame.Close())
        sendFrameChannel.close()
        //close the message channel as well, since it is idempotent there is no harm
        receiveMessageChannel.close()
      }

    }
  }
}
