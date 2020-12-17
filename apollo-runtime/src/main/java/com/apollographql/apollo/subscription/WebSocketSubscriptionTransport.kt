package com.apollographql.apollo.subscription

import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.Utils
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okio.Buffer
import java.lang.ref.WeakReference
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference

/**
 * [SubscriptionTransport] implementation based on [WebSocket].
 */
class WebSocketSubscriptionTransport @JvmOverloads constructor(
    private val webSocketRequest: Request,
    private val webSocketConnectionFactory: WebSocket.Factory,
    private val callback: SubscriptionTransport.Callback,
    private val writePayloadAsJsonString: Boolean = false,
    private val extensions: Map<String, Any?> = emptyMap()
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

  private fun release() {
    webSocketListener.getAndSet(null)?.release()
    webSocket.set(null)
  }

  private fun OperationClientMessage.serializeToJson(): String =
      toJsonString(
        writePayloadAsJsonString = writePayloadAsJsonString,
        extensions = extensions
    )

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

  /**
   * @param webSocketUrl The URL of the GraphQL WebSocket API
   * @param webSocketConnectionFactory The [WebSocket.Factory] to use to create the websockets
   * @param writePayloadAsJsonString If the payload should be serialized as a JSON String (default false). Set this to `true` for AWS
   *                                 AppSync integrations.
   * @param extensions Any additional extensions to when sending start commands. Must not be changed after this creating the factory.
   */
  class Factory @JvmOverloads constructor(
      webSocketUrl: String,
      private val webSocketConnectionFactory: WebSocket.Factory,
      private val writePayloadAsJsonString: Boolean = false,
      private val extensions: Map<String, Any?> = emptyMap()
  ) : SubscriptionTransport.Factory {
    private val webSocketRequest: Request = Request.Builder()
        .url(webSocketUrl)
        .addHeader("Sec-WebSocket-Protocol", "graphql-ws")
        .addHeader("Cookie", "")
        .build()

    override fun create(callback: SubscriptionTransport.Callback): SubscriptionTransport =
        WebSocketSubscriptionTransport(webSocketRequest, webSocketConnectionFactory, callback, writePayloadAsJsonString, extensions)
  }

  class AppSyncFactory @JvmOverloads constructor(
      webSocketUrl: String,
      private val webSocketConnectionFactory: WebSocket.Factory,
      authorization: Map<String, Any?>,
      payload: Map<String, Any?> = emptyMap()
  ) : SubscriptionTransport.Factory by Factory(
      webSocketUrl = webSocketUrl
          .let {
            HttpUrl.get(when {
              it.startsWith("ws://", ignoreCase = true) -> "http" + it.drop(2)
              it.startsWith("wss://", ignoreCase = true) -> "https" + it.drop(3)
              else -> it
            })
          }
          .newBuilder()
          .setQueryParameter("header", authorization.encodeAsQueryParam())
          .setQueryParameter("payload", payload.encodeAsQueryParam())
          .build()
          .toString(),
      webSocketConnectionFactory = webSocketConnectionFactory,
      writePayloadAsJsonString = true,
      extensions = mapOf("authorization" to authorization)
  ) {
    companion object {
      private fun Map<String, Any?>.encodeAsQueryParam(): String {
        val buffer = Buffer()
        Utils.writeToJson(this, JsonWriter.of(buffer))
        return Base64.getUrlEncoder().encodeToString(buffer.readByteArray())
      }
    }
  }
}