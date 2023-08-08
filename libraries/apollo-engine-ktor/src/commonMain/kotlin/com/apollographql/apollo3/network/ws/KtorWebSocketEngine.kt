package com.apollographql.apollo3.network.ws

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.exception.ApolloWebSocketClosedException
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
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

  private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

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
    coroutineScope.launch {
      try {
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
          launch {
            while (true) {
              val frame = sendFrameChannel.receive()
              try {
                send(frame)
              } catch (e: Exception) {
                val closeReason = try {closeReason.await()} catch (e: Exception) {null}
                receiveMessageChannel.close(ApolloWebSocketClosedException(code = closeReason?.code?.toInt()
                    ?: -1, reason = closeReason?.message, cause = e))
                sendFrameChannel.close(e)
                break
              }
            }
          }
          while (true) {
            when (val frame = try {
              incoming.receive()
            } catch (e: ClosedReceiveChannelException) {
              val closeReason = try {closeReason.await()} catch (e: Exception) {null}
              receiveMessageChannel.close(ApolloWebSocketClosedException(code = closeReason?.code?.toInt()
                  ?: -1, reason = closeReason?.message, cause = e))
              sendFrameChannel.close(e)
              break
            }) {
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
      } catch (e: Exception) {
        receiveMessageChannel.close(e)
        sendFrameChannel.close(e)
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
      }

    }
  }
}
