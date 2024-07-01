package com.apollographql.apollo.network.ws

import com.apollographql.apollo.api.http.HttpHeader
import io.ktor.http.Headers
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.util.PlatformUtils
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.ByteString
import org.khronos.webgl.Uint8Array
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket
import org.w3c.dom.events.Event
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class DefaultWebSocketEngine : WebSocketEngine {

  actual override suspend fun open(
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
    val socket = createWebSocket(newUrl.toString(), Headers.build { headers.forEach { append(it.name, it.value) } }).awaitConnection()
    val messageChannel = Channel<String>(Channel.UNLIMITED)
    socket.onmessage = { messageEvent: MessageEvent ->
      val data = messageEvent.data
      if (data != null) {
        when (data) {
          is String -> messageChannel.trySend(data)
          else -> throw UnsupportedOperationException("${data::class.simpleName} is currently unsupported")
        }
      }
    }
    socket.onclose = {
      messageChannel.close()
    }
    return object : WebSocketConnection {
      override suspend fun receive(): String {
        return messageChannel.receive()
      }

      override fun send(data: ByteString) {
        socket.send(Uint8Array(data.toByteArray().toTypedArray()))
      }

      override fun send(string: String) {
        socket.send(string)
      }

      override fun close() {
        socket.close(CLOSE_NORMAL.toShort())
        //close the message channel as well, since it is idempotent there is no harm
        messageChannel.close()
      }
    }
  }

  /*
   * The function below works due to ktor being used for the HTTP engine
   * and how ktor imports ws, if this changes, ws would need to be added
   * as a direct dependency.
   *
   * The following applies for lines below
   * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
   * Some lines have been added/modified from the original source (https://github.com/ktorio/ktor/blob/f723638afd4d36024c390c5b79108b53ab513943/ktor-client/ktor-client-core/js/src/io/ktor/client/engine/js/JsClientEngine.kt#L62)
   * in order to fix an issue with subprotocols on Node (https://youtrack.jetbrains.com/issue/KTOR-4001)
   */
  // Adding "_capturingHack" to reduce chances of JS IR backend to rename variable,
  // so it can be accessed inside js("") function
  @Suppress("UNUSED_PARAMETER", "UnsafeCastFromDynamic", "UNUSED_VARIABLE", "LocalVariableName")
  private fun createWebSocket(urlString_capturingHack: String, headers: Headers): WebSocket {
    val (protocolHeaderNames, otherHeaderNames) = headers.names().partition { it.equals("sec-websocket-protocol", true) }
    val protocols = protocolHeaderNames.mapNotNull { headers.getAll(it) }.flatten().toTypedArray()
    return if (PlatformUtils.IS_NODE) {
      val ws_capturingHack = js("eval('require')('ws')")
      val headers_capturingHack: dynamic = object {}
      headers.forEach { name, values ->
        headers_capturingHack[name] = values.joinToString(",")
      }
      js("new ws_capturingHack(urlString_capturingHack, protocols, { headers: headers_capturingHack })")
    } else {
      check(otherHeaderNames.isEmpty()) {
        "Apollo: the WebSocket browser API doesn't allow passing headers. Use connectionPayload or other mechanisms."
      }
      js("new WebSocket(urlString_capturingHack, protocols)")
    }
  }

  private suspend fun WebSocket.awaitConnection(): WebSocket = suspendCancellableCoroutine { continuation ->
    if (continuation.isCancelled) return@suspendCancellableCoroutine

    var eventListener: ((Event) -> Unit)? = null
    val removeEventListeners = {
      removeEventListener("open", callback = eventListener)
      removeEventListener("error", callback = eventListener)
    }

    eventListener = { event: Event ->
      if (!continuation.isCancelled) {
        when (event.type) {
          "open" -> {
            continuation.resume(this)
            /**
             * If the websocket fires an error after it has been connected we don't want to
             * pick that up here because we've already resumed the coroutine.
             */
            removeEventListeners()
          }
          "error" -> {
            continuation.resumeWithException(IllegalStateException(event.asString()))
          }
        }
      }
    }

    continuation.invokeOnCancellation {
      removeEventListeners()

      if (it != null) {
        this@awaitConnection.close()
      }
    }

    addEventListener("open", callback = eventListener)
    addEventListener("error", callback = eventListener)
  }

  private fun Event.asString(): String = buildString {
    append(JSON.stringify(this@asString, arrayOf("message", "target", "type", "isTrusted")))
  }
}


