package com.apollographql.apollo.network.websocket

import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.exception.DefaultApolloException
import com.apollographql.apollo.internal.isNode
import com.apollographql.apollo.network.http.setTimeout
import node.buffer.Buffer
import org.khronos.webgl.Uint8Array
import org.w3c.dom.WebSocket as PlatformWebSocket

internal class JsWebSocketEngine: WebSocketEngine {
  override fun newWebSocket(url: String, headers: List<HttpHeader>, listener: WebSocketListener): WebSocket {
    return JsWebSocket(url, headers, listener)
  }

  override fun close() {
  }
}

internal class JsWebSocket(
    url: String,
    headers: List<HttpHeader>,
    private val listener: WebSocketListener,
) : WebSocket {
  private val platformWebSocket: PlatformWebSocket?
  private var disposed = false

  private val actualUrl = when {
    url.startsWith("http://") -> "ws://${url.substring(7)}"
    url.startsWith("https://") -> "wss://${url.substring(8)}"
    else -> url
  }
  init {
    platformWebSocket = createWebSocket(actualUrl, headers, listener)
    platformWebSocket?.onopen = {
      listener.onOpen()
    }

    platformWebSocket?.onmessage = {
      val data2: dynamic = it.data

      @Suppress("USELESS_CAST")
      when {
        data2 is String -> listener.onMessage(data2 as String)
        Buffer.isBuffer(data2) -> {
          listener.onMessage((data2 as Buffer).toByteArray())
        }
        else -> {
          val d = JSON.stringify(data2)
          if (!disposed) {
            disposed = true
            listener.onError(DefaultApolloException("Apollo: unsupported message received ('$d')"))
            platformWebSocket?.close(CLOSE_GOING_AWAY.toShort(), "Unsupported message received")
          }
          Unit
        }
      }
    }

    platformWebSocket?.onerror = {
      if (!disposed) {
        disposed = true
        listener.onError(DefaultApolloException("Error while reading websocket"))
      }
    }

    platformWebSocket?.onclose = {
      val event: dynamic = it
      if (!disposed) {
        disposed = true
        if (event.wasClean) {
          listener.onClosed(event.code, event.reason)
        } else {
          listener.onError(DefaultApolloException("WebSocket was closed"))
        }
      }
    }
  }

  override fun send(data: ByteArray) {
    platformWebSocket?.let {
      if (it.bufferedAmount.toDouble() > MAX_BUFFERED) {
        if (!disposed) {
          disposed = true
          listener.onError(DefaultApolloException("Too much data queued"))
        }
      }
      it.send(Uint8Array(data.toTypedArray()))
    }
  }

  override fun send(text: String) {
    platformWebSocket?.let {
      if (it.bufferedAmount.toDouble() > MAX_BUFFERED) {
        if (!disposed) {
          disposed = true
          listener.onError(DefaultApolloException("Too much data queued"))
        }
      }

      it.send(text)
    }
  }

  override fun close(code: Int, reason: String) {
    if (!disposed) {
      disposed = true
      platformWebSocket?.close(code.toShort(), reason)
    }
  }
}

/**
 * This is probably far too high and problems will probably appear much sooner but this is used to signal the intent
 * of this code as well as a last resort
 */
private val MAX_BUFFERED = 100_000_000

/*
 * The following applies for lines below
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 * Some lines have been added/modified from the original source (https://github.com/ktorio/ktor/blob/f723638afd4d36024c390c5b79108b53ab513943/ktor-client/ktor-client-core/js/src/io/ktor/client/engine/js/JsClientEngine.kt#L62)
 * in order to fix an issue with subprotocols on Node (https://youtrack.jetbrains.com/issue/KTOR-4001)
 */
/**
 * Adding "_capturingHack" to reduce chances of JS IR backend to rename variable,
 * so it can be accessed inside js("") function
 *
 * May return null if headers are set
 */
@Suppress("UNUSED_PARAMETER", "UnsafeCastFromDynamic", "UNUSED_VARIABLE", "LocalVariableName")
private fun createWebSocket(urlString_capturingHack: String, headers: List<HttpHeader>, listener: WebSocketListener): PlatformWebSocket? {
  val (protocolHeaders, otherHeaders) = headers.partition { it.name.equals("sec-websocket-protocol", true) }
  val protocols = protocolHeaders.map { it.value }.toTypedArray()
  return if (isNode) {
    val ws_capturingHack = js("eval('require')('ws')")
    val headers_capturingHack: dynamic = object {}
    headers.forEach {
      headers_capturingHack[it.name] = it.value
    }
    js("new ws_capturingHack(urlString_capturingHack, protocols, { headers: headers_capturingHack })")
  } else {
    if(otherHeaders.isNotEmpty()) {
      // Escape the current stack frame
      setTimeout({
        listener.onError(DefaultApolloException("Apollo: the WebSocket browser API doesn't allow passing headers. Use connectionPayload or other mechanisms."))
      }, 10)
      null
    } else {
      js("new WebSocket(urlString_capturingHack, protocols)")
    }
  }
}

actual fun WebSocketEngine(): WebSocketEngine = JsWebSocketEngine()
