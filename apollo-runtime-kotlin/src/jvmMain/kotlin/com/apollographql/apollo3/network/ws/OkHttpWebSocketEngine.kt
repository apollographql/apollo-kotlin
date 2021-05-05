package com.apollographql.apollo3.network.ws

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString

@ExperimentalCoroutinesApi
actual class DefaultWebSocketEngine(
    private val webSocketFactory: WebSocket.Factory
) : WebSocketEngine {

  actual constructor() : this(
      webSocketFactory = OkHttpClient()
  )

  override suspend fun open(
      url: String,
      headers: Map<String, String>
  ): WebSocketConnection {
    val messageChannel = Channel<ByteString>(Channel.BUFFERED)
    val webSocketOpenResult = CompletableDeferred<Unit>()

    val request = Request.Builder()
        .url(url)
        .headers(Headers.of(headers))
        .build()

    val webSocket = webSocketFactory.newWebSocket(request, object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        webSocketOpenResult.complete(Unit)
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        println("receivedText: ${text}")
        runBlocking {
          kotlin.runCatching {
            messageChannel.send(text.toByteArray().toByteString())
          }
        }
      }

      override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        println("receivedBytes: ${bytes.utf8()}")
        runBlocking {
          kotlin.runCatching {
            messageChannel.send(bytes)
          }
        }
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        webSocketOpenResult.complete(Unit)
        messageChannel.close(t)
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        messageChannel.close()
      }
    })

    webSocketOpenResult.await()

    messageChannel.invokeOnClose {
      // I think this is not necessary. The caller must call [WebSocketConnection.close] in all cases.
      // This should either trigger onClose or onFailure which should close the messageChannel
      //
      // Since this is idempotent, it shouldn't harm too much to keep it
      webSocket.close(CLOSE_GOING_AWAY, null)
    }

    return object : WebSocketConnection {
      override suspend fun receive(): ByteString {
        return messageChannel.receive()
      }

      /**
       * OkHttp always succeeds
       */
      override suspend fun send(data: ByteString) {
        println("send: ${data.utf8()}")
        while(!webSocket.send(data)) {
          delay(100)
        }
      }

      override fun close() {
        webSocket.close(CLOSE_NORMAL, null)
      }
    }
  }
}
