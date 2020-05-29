package com.apollographql.apollo.network.websocket

import com.apollographql.apollo.ApolloWebSocketException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import okio.internal.commonAsUtf8ToByteArray

@ExperimentalCoroutinesApi
actual class ApolloWebSocketFactory(
    private val request: Request,
    private val webSocketFactory: WebSocket.Factory
) {

  actual constructor(
      serverUrl: String,
      headers: Map<String, String>
  ) : this(
      request = Request.Builder()
          .url(serverUrl.toHttpUrl())
          .headers(headers.toHeaders())
          .build(),
      webSocketFactory = OkHttpClient()
  )

  actual suspend fun open(): ApolloWebSocketConnection {
    val messageChannel = Channel<ByteString>(Channel.CONFLATED)
    val webSocketConnectionDeferred = CompletableDeferred<WebSocket>()

    val webSocket = webSocketFactory.newWebSocket(request = request, listener = object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        if (!webSocketConnectionDeferred.complete(webSocket)) {
          webSocket.cancel()
        }
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        try {
          messageChannel.offer(text.commonAsUtf8ToByteArray().toByteString())
        } catch (e: Exception) {
          webSocket.cancel()
        }
      }

      override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        try {
          messageChannel.offer(bytes)
        } catch (e: Exception) {
          webSocket.cancel()
        }
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        messageChannel.close(
            ApolloWebSocketException(
                message = "Web socket communication error",
                cause = t
            )
        )
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        messageChannel.close()
      }
    })

    try {
      return ApolloWebSocketConnection(
          webSocket = webSocketConnectionDeferred.await(),
          messageChannel = messageChannel
      )
    } finally {
      webSocket.cancel()
    }
  }
}

@ExperimentalCoroutinesApi
actual class ApolloWebSocketConnection(
    private val webSocket: WebSocket,
    private val messageChannel: Channel<ByteString> = Channel()
) : ReceiveChannel<ByteString> by messageChannel {

  init {
    messageChannel.invokeOnClose {
      webSocket.close(code = 1000, reason = null)
    }
  }

  actual fun send(data: ByteString) {
    if (!messageChannel.isClosedForReceive) {
      webSocket.send(data)
    }
  }

  actual fun close() {
    messageChannel.close()
  }
}
