package com.apollographql.apollo3.network.ws

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_2_2
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.internal.ChannelWrapper
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
    val socket = createWebSocket(newUrl.toString(), Headers.build { headers.forEach { append(it.name, it.value) } }).awaitConnection()
    val messageChannel = ChannelWrapper(Channel<String>(Channel.UNLIMITED))
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

  @Deprecated(
      "Use open(String, List<HttpHeader>) instead.",
      ReplaceWith(
          "open(url, headers.map { HttpHeader(it.key, it.value })",
          "com.apollographql.apollo3.api.http.HttpHeader"
      )
  )
  @ApolloDeprecatedSince(v3_2_2)
  override suspend fun open(url: String, headers: Map<String, String>): WebSocketConnection =
      open(url, headers.map { HttpHeader(it.key, it.value) })

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
  private fun createWebSocket(urlString_capturingHack: String, headers: Headers): WebSocket =
      if (PlatformUtils.IS_NODE) {
        val ws_capturingHack = js("eval('require')('ws')")
        val headers_capturingHack: dynamic = object {}
        headers.forEach { name, values ->
          headers_capturingHack[name] = values.joinToString(",")
        }
        val protocolHeaderNames = headers.names().filter { it.equals("sec-websocket-protocol", true) }
        val protocols = protocolHeaderNames.mapNotNull { headers.getAll(it) }.flatten().toTypedArray()
        js("new ws_capturingHack(urlString_capturingHack, protocols, { headers: headers_capturingHack })")
      } else {
        js("new WebSocket(urlString_capturingHack)")
      }

  private suspend fun WebSocket.awaitConnection(): WebSocket = suspendCancellableCoroutine { continuation ->
    if (continuation.isCancelled) return@suspendCancellableCoroutine

    val eventListener = { event: Event ->
      when (event.type) {
        "open" -> continuation.resume(this)
        "error" -> {
          continuation.resumeWithException(IllegalStateException(event.asString()))
        }
      }
    }

    addEventListener("open", callback = eventListener)
    addEventListener("error", callback = eventListener)

    continuation.invokeOnCancellation {
      removeEventListener("open", callback = eventListener)
      removeEventListener("error", callback = eventListener)

      if (it != null) {
        this@awaitConnection.close()
      }
    }
  }

  private fun Event.asString(): String = buildString {
    append(JSON.stringify(this@asString, arrayOf("message", "target", "type", "isTrusted")))
  }
}


