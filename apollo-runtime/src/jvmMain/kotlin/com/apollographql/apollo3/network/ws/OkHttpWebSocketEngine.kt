package com.apollographql.apollo3.network.ws

import com.apollographql.apollo3.exception.ApolloWebSocketClosedException
import com.apollographql.apollo3.internal.ChannelWrapper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class OkHttpWebSocketEngine(
    private val webSocketFactory: WebSocket.Factory,
) : WebSocketEngine {

  constructor() : this(
      webSocketFactory = OkHttpClient()
  )

  override suspend fun open(
      url: String,
      headers: Map<String, String>,
  ): WebSocketConnection {
    val messageChannel = ChannelWrapper(Channel<String>(Channel.UNLIMITED))
    val webSocketOpenResult = CompletableDeferred<Unit>()

    //println("opening $url")
    val request = Request.Builder()
        .url(url)
        .headers(Headers.of(headers))
        .build()

    val webSocket = webSocketFactory.newWebSocket(request, object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        webSocketOpenResult.complete(Unit)
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        //println("receivedText: ${text}")
        messageChannel.trySend(text)
      }

      override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        //println("receivedBytes: ${bytes.utf8()}")
        messageChannel.trySend(bytes.utf8())
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        //println("onFailure: ${t.message} - ${response?.body()?.string()}")
        webSocketOpenResult.complete(Unit)
        messageChannel.close(t)
      }

      override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
        webSocketOpenResult.complete(Unit)

        val t = ApolloWebSocketClosedException(code, reason)
        messageChannel.close(t)
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        messageChannel.close()
      }
    })

    webSocketOpenResult.await()

    messageChannel.setInvokeOnClose {
      // I think this is not necessary. The caller must call [WebSocketConnection.close] in all cases.
      // This should either trigger onClose or onFailure which should close the messageChannel
      //
      // Since this is idempotent, it shouldn't harm too much to keep it
      webSocket.close(CLOSE_GOING_AWAY, null)
    }

    return object : WebSocketConnection {
      override suspend fun receive(): String {
        return messageChannel.receive()
      }

      override fun send(data: ByteString) {
        //println("sendBytes: ${data.utf8()}")
        check(webSocket.send(data)) {
          "WeSocket queue full"
        }
      }

      override fun send(string: String) {
        //println("sendText: $string")
        check(webSocket.send(string)) {
          "WeSocket queue full"
        }
      }

      override fun close() {
        webSocket.close(CLOSE_NORMAL, null)
      }
    }
  }
}

@Suppress("FunctionName")
actual fun WebSocketEngine(): WebSocketEngine = OkHttpWebSocketEngine()
