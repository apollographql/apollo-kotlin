package com.apollographql.apollo3.subscription

import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okio.Buffer
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

/**
 * [SubscriptionTransport] implementation based on [WebSocket].
 */
class WebSocketSubscriptionTransport @JvmOverloads constructor(
    private val webSocketRequest: Request,
    private val webSocketConnectionFactory: WebSocket.Factory,
    private val callback: SubscriptionTransport.Callback,
    private val serializer: OperationMessageSerializer = ApolloOperationMessageSerializer
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
        ?.close(1001, message.serializeToJson())
    release()
  }

  override fun send(message: OperationClientMessage) {
    val socket = webSocket.get() ?: run {
      callback.onFailure(IllegalStateException("Send attempted on closed connection"))
      return
    }
    socket.send(message.serializeToJson())
  }

  internal fun onOpen() {
    callback.onConnected()
  }

  internal fun onMessage(message: OperationServerMessage) {
    callback.onMessage(message)
  }

  internal fun onFailure(t: Throwable) {
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

  private fun release() {
    webSocketListener.getAndSet(null)?.release()
    webSocket.set(null)
  }

  private fun OperationClientMessage.serializeToJson(): String {
    val buffer = Buffer()
    serializer.writeClientMessage(this, buffer)
    return buffer.readUtf8()
  }

  internal class WebSocketListener(delegate: WebSocketSubscriptionTransport) : okhttp3.WebSocketListener() {
    private val delegateRef = WeakReference(delegate)

    override fun onOpen(webSocket: WebSocket, response: Response) {
      delegateRef.get()?.onOpen()
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
      val delegate = delegateRef.get() ?: return
      delegate.onMessage(delegate.serializer.readServerMessage(Buffer().writeUtf8(text)))
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

  /**
   * @param webSocketUrl The URL of the GraphQL Web Socket API.
   * @param webSocketConnectionFactory The [WebSocket.Factory] to use to create the websockets.
   * @param serializer A [OperationMessageSerializer] that will be used to read and write messages.
   */
  class Factory @JvmOverloads constructor(
      webSocketUrl: String,
      private val webSocketConnectionFactory: WebSocket.Factory,
      private val serializer: OperationMessageSerializer = ApolloOperationMessageSerializer
  ) : SubscriptionTransport.Factory {
    private val webSocketRequest: Request = Request.Builder()
        .url(webSocketUrl)
        .addHeader("Sec-WebSocket-Protocol", "graphql-ws")
        .addHeader("Cookie", "")
        .build()

    override fun create(callback: SubscriptionTransport.Callback): SubscriptionTransport =
        WebSocketSubscriptionTransport(webSocketRequest, webSocketConnectionFactory, callback, serializer)
  }
}