package com.apollographql.apollo.network.websocket

import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.exception.DefaultApolloException
import org.w3c.dom.WebSocket as PlatformWebSocket

/**
 * WebSocket implementation for wasmJs platform.
 * 
 * This implementation has several limitations due to wasmJs platform constraints:
 * - Only browser WebSocket API is supported (no Node.js)
 * - Custom HTTP headers are not supported (browser WebSocket API limitation)
 * - Binary data is converted to UTF-8 strings with a data URI prefix
 * - Multiple protocols are simplified to use only the first one
 * - Complex js() function calls are split into simple helper functions
 * 
 * For authentication, use connectionPayload or URL-based authentication instead of headers.
 */

// Top-level helper functions for wasmJs js() call restrictions
private fun createWebSocketSimple(url: String): PlatformWebSocket = js("new WebSocket(url)")
private fun createWebSocketWithProtocol(url: String, protocol: String): PlatformWebSocket = js("new WebSocket(url, protocol)")

internal class WasmJsWebSocketEngine: WebSocketEngine {
  override fun newWebSocket(url: String, headers: List<HttpHeader>, listener: WebSocketListener): WebSocket {
    return WasmJsWebSocket(url, headers, listener)
  }

  override fun close() {
    // Nothing to clean up for wasmJs implementation
  }
}

internal class WasmJsWebSocket(
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

    platformWebSocket?.onmessage = { event ->
      val data = event.data
      if (data != null) {
        // For wasmJs, we assume all messages are strings since binary data handling is limited
        val stringData = data.toString()
        listener.onMessage(stringData)
      }
    }

    platformWebSocket?.onerror = {
      if (!disposed) {
        disposed = true
        listener.onError(DefaultApolloException("Apollo: Error while reading websocket"))
      }
    }

    platformWebSocket?.onclose = { event ->
      if (!disposed) {
        disposed = true
        // For wasmJs, we simplify close handling since we can't access dynamic properties
        listener.onClosed(null, null)
      }
    }
  }

  override fun send(data: ByteArray) {
    platformWebSocket?.let { socket ->
      if (socket.bufferedAmount.toDouble() > MAX_BUFFERED) {
        if (!disposed) {
          disposed = true
          listener.onError(DefaultApolloException("Apollo: Too much data queued"))
        }
      }
      // For wasmJs, convert binary data to UTF-8 string as a workaround
      // This is a limitation of wasmJs WebSocket implementation - binary data support is restricted
      // The receiving end should handle the "data:application/octet-stream;charset=utf-8," prefix
      val dataString = data.decodeToString()
      socket.send("data:application/octet-stream;charset=utf-8,$dataString")
    }
  }

  override fun send(text: String) {
    platformWebSocket?.let {
      if (it.bufferedAmount.toDouble() > MAX_BUFFERED) {
        if (!disposed) {
          disposed = true
          listener.onError(DefaultApolloException("Apollo: Too much data queued"))
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

private fun createWebSocket(url: String, headers: List<HttpHeader>, listener: WebSocketListener): PlatformWebSocket? {
  val (protocolHeaders, otherHeaders) = headers.partition { it.name.equals("sec-websocket-protocol", true) }
  val protocols = protocolHeaders.map { it.value }.toTypedArray()
  
  // For wasmJs, we only support browser environment (no Node.js)
  if (otherHeaders.isNotEmpty()) {
    // Immediately call error callback - headers not supported in browser WebSocket API
    listener.onError(DefaultApolloException("Apollo: the WebSocket browser API doesn't allow passing headers. Use connectionPayload or other mechanisms."))
    return null
  } else {
    return when {
      protocols.isEmpty() -> createWebSocketSimple(url)
      protocols.size == 1 -> createWebSocketWithProtocol(url, protocols[0])
      else -> {
        // For multiple protocols, just use the first one as wasmJs has limitations
        createWebSocketWithProtocol(url, protocols[0])
      }
    }
  }
}

actual fun WebSocketEngine(): WebSocketEngine = WasmJsWebSocketEngine()