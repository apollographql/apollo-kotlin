package com.apollographql.apollo.subscription

import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

/**
 * [SubscriptionTransport] implementation based on [WebSocket].
 */
class WebSocketSubscriptionTransport(
    private val webSocketRequest: Request,
    private val webSocketConnectionFactory: WebSocket.Factory,
    private val callback: SubscriptionTransport.Callback
) : SubscriptionTransport {
  internal val webSocket = AtomicReference<WebSocket>()
  internal val webSocketListener = AtomicReference<WebSocketListener>()

  override fun connect() {
    val webSocketListener = WebSocketListener(this)
    check(this.webSocketListener.compareAndSet(null, webSocketListener)) {
      "Already connected"
    }
    webSocket.set(webSocketConnectionFactory.newWebSocket(webSocketRequest, webSocketListener))
  }

  override fun disconnect(message: OperationClientMessage) {
    webSocket.getAndSet(null)
        ?.close(1001, message.toJsonString())
    release()
  }

  override fun send(message: OperationClientMessage) {
    val socket = webSocket.get() ?: run {
      callback.onFailure(IllegalStateException("Send attempted on closed connection"))
      return
    }
    socket.send(message.toJsonString())
  }

  internal fun onOpen() {
    callback.onConnected()
  }

  internal fun onMessage(message: OperationServerMessage?) {
    callback.onMessage(message)
  }

  internal fun onFailure(t: Throwable?) {
    try {
      callback.onFailure(t)
    } finally {
      release()
    }
  }

  internal fun onClosed() {
    try {
      callback.onClosed()
    } finally {
      release()
    }
  }

  internal fun release() {
    webSocketListener.getAndSet(null)?.release()
    webSocket.set(null)
  }

  internal class WebSocketListener(delegate: WebSocketSubscriptionTransport) : okhttp3.WebSocketListener() {
    private val delegateRef = WeakReference(delegate)

    override fun onOpen(webSocket: WebSocket, response: Response) {
      delegateRef.get()?.onOpen()
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
      delegateRef.get()?.onMessage(OperationServerMessage.fromJsonString(text))
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
      delegateRef.get()?.onFailure(t)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
      delegateRef.get()?.onClosed()
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
      delegateRef.get()?.onClosed()
    }

    fun release() {
      delegateRef.clear()
    }
  }

  class Factory(webSocketUrl: String, private val webSocketConnectionFactory: WebSocket.Factory) : SubscriptionTransport.Factory {
    private val webSocketRequest: Request = Request.Builder()
        .url(webSocketUrl)
        .addHeader("Sec-WebSocket-Protocol", "graphql-ws")
        .addHeader("Cookie", "")
        .build()

    override fun create(callback: SubscriptionTransport.Callback): SubscriptionTransport =
        WebSocketSubscriptionTransport(webSocketRequest, webSocketConnectionFactory, callback)
  }
}