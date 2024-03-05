package com.apollographql.apollo3.network.ws.incubating

import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.exception.ApolloNetworkException
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.WebSocket as PlatformWebSocket
import okhttp3.WebSocketListener as PlatformWebSocketListener

internal class JvmWebSocketEngine(private val okHttpClient: OkHttpClient) : WebSocketEngine {
  override fun newWebSocket(url: String, headers: List<HttpHeader>, listener: WebSocketListener): WebSocket {
    return JvmWebSocket(okHttpClient, url, headers, listener)
  }

  override fun close() {

  }
}

internal class JvmWebSocket(
    webSocketFactory: PlatformWebSocket.Factory,
    url: String,
    headers: List<HttpHeader>,
    private val listener: WebSocketListener,
) : WebSocket, PlatformWebSocketListener() {
  private val platformWebSocket: PlatformWebSocket
  private val disposed = AtomicBoolean(false)

  init {
    val request = Request.Builder()
        .url(url)
        .headers(headers.toOkHttpHeaders())
        .build()

    platformWebSocket = webSocketFactory.newWebSocket(request, this)
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
      platformWebSocket.cancel()
    }
  }

  override fun onClosing(webSocket: PlatformWebSocket, code: Int, reason: String) {
    if (disposed.compareAndSet(false, true)) {
      listener.onClosed(code, reason)
      platformWebSocket.cancel()
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
    platformWebSocket.send(data.toByteString())
  }

  override fun send(text: String) {
    platformWebSocket.send(text)
  }

  override fun close(code: Int, reason: String) {
    if (disposed.compareAndSet(false, true)) {
      platformWebSocket.close(code, reason)
    }
  }
}


actual fun WebSocketEngine(): WebSocketEngine = JvmWebSocketEngine(defaultOkHttpClientBuilder.build())