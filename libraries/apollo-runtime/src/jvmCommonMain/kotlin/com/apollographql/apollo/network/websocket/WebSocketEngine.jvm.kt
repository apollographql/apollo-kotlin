package com.apollographql.apollo.network.websocket

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.network.defaultOkHttpClientBuilder
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import okhttp3.WebSocket as PlatformWebSocket
import okhttp3.WebSocketListener as PlatformWebSocketListener

internal class JvmWebSocketEngine(private val webSocketFactory: PlatformWebSocket.Factory) : WebSocketEngine {
  var closed = false
  override fun newWebSocket(url: String, headers: List<HttpHeader>, listener: WebSocketListener): WebSocket {
    require(!closed) {
      "JvmWebSocketEngine is closed"
    }
    return JvmWebSocket(webSocketFactory, url, headers, listener)
  }

  override fun close() {
    closed = true
  }
}

internal class JvmWebSocket(
    webSocketFactory: PlatformWebSocket.Factory,
    url: String,
    headers: List<HttpHeader>,
    private val listener: WebSocketListener,
) : WebSocket, PlatformWebSocketListener() {
  /**
   * There's a race where the listener might be called from another thread beform [platformWebSocket] gets initialized
   */
  private val platformWebSocket: AtomicReference<PlatformWebSocket> = AtomicReference()
  private val disposed = AtomicBoolean(false)

  init {
    val request = Request.Builder()
        .url(url)
        .headers(headers.toOkHttpHeaders())
        .build()

    platformWebSocket.set(webSocketFactory.newWebSocket(request, this))
  }

  override fun onOpen(webSocket: PlatformWebSocket, response: Response) {
    listener.onOpen()
  }

  override fun onMessage(webSocket: PlatformWebSocket, bytes: ByteString) {
    listener.onMessage(bytes.toByteArray())
  }

  override fun onMessage(webSocket: PlatformWebSocket, text: String) {
    listener.onMessage(text)
  }

  override fun onFailure(webSocket: PlatformWebSocket, t: Throwable, response: Response?) {
    if (disposed.compareAndSet(false, true)) {
      listener.onError(ApolloNetworkException(t.message, t))
      platformWebSocket.get()?.cancel()
    }
  }

  override fun onClosing(webSocket: PlatformWebSocket, code: Int, reason: String) {
    if (disposed.compareAndSet(false, true)) {
      listener.onClosed(code, reason)
      /**
       * Acknowledge the close
       * Note: just calling cancel() here leaks the connection
       */
      platformWebSocket.get()?.close(code, reason)
    }
  }

  override fun onClosed(webSocket: PlatformWebSocket, code: Int, reason: String) {
    /**
     * Do nothing. When we come here either [close] or [onClosing] has been called and the [WebSocket]
     * is disposed already.
     */
  }

  private fun List<HttpHeader>.toOkHttpHeaders(): Headers =
      Headers.Builder().also { headers ->
        this.forEach {
          headers.add(it.name, it.value)
        }
      }.build()

  override fun send(data: ByteArray) {
    platformWebSocket.get()?.send(data.toByteString())
  }

  override fun send(text: String) {
    platformWebSocket.get()?.send(text)
  }

  override fun close(code: Int, reason: String) {
    if (disposed.compareAndSet(false, true)) {
      platformWebSocket.get()?.close(code, reason)
    }
  }
}


actual fun WebSocketEngine(): WebSocketEngine = JvmWebSocketEngine(defaultOkHttpClientBuilder.build())

@ApolloExperimental
fun WebSocketEngine(webSocketFactory: PlatformWebSocket.Factory): WebSocketEngine = JvmWebSocketEngine(webSocketFactory)