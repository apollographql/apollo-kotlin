package com.apollographql.apollo.network.ws

import com.apollographql.apollo.api.http.HttpHeader
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.ByteString
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket
import org.w3c.dom.events.Event
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Top-level helper functions for wasmJs js() call restrictions
private fun createWebSocketSimple(url: String): WebSocket = js("new WebSocket(url)")
private fun createWebSocketWithProtocol(url: String, protocol: String): WebSocket = js("new WebSocket(url, protocol)")

actual class DefaultWebSocketEngine actual constructor() : WebSocketEngine {
  actual override suspend fun open(
      url: String,
      headers: List<HttpHeader>,
  ): WebSocketConnection {
    // Convert HTTP/HTTPS URLs to WS/WSS
    val wsUrl = when {
      url.startsWith("http://") -> "ws://${url.substring(7)}"
      url.startsWith("https://") -> "wss://${url.substring(8)}"
      else -> url
    }
    
    // Extract WebSocket protocols from headers
    val protocols = headers
      .filter { it.name.equals("sec-websocket-protocol", ignoreCase = true) }
      .map { it.value }
      .toTypedArray()
    
    // Check for non-protocol headers (not supported in browser WebSocket API)
    val nonProtocolHeaders = headers.filter { !it.name.equals("sec-websocket-protocol", ignoreCase = true) }
    if (nonProtocolHeaders.isNotEmpty()) {
      throw IllegalArgumentException("Apollo: the WebSocket browser API doesn't allow passing headers. Use connectionPayload or other mechanisms.")
    }
    
    val socket = createWebSocket(wsUrl, protocols).awaitConnection()
    val messageChannel = Channel<String>(Channel.UNLIMITED)
    
    socket.onmessage = { messageEvent: MessageEvent ->
      val data = messageEvent.data
      if (data != null) {
        // For wasmJs, we assume all messages are strings since binary data handling is limited
        val stringData = data.toString()
        messageChannel.trySend(stringData)
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
        // For wasmJs, convert binary data to base64 string as a workaround
        // This is a limitation of wasmJs WebSocket implementation
        val base64String = data.base64()
        socket.send("data:application/octet-stream;base64,$base64String")
      }

      override fun send(string: String) {
        socket.send(string)
      }

      override fun close() {
        socket.close(CLOSE_NORMAL.toShort())
        messageChannel.close()
      }
    }
  }

  private fun createWebSocket(url: String, protocols: Array<String>): WebSocket {
    return when {
      protocols.isEmpty() -> createWebSocketSimple(url)
      protocols.size == 1 -> createWebSocketWithProtocol(url, protocols[0])
      else -> {
        // For multiple protocols, just use the first one as wasmJs has limitations
        createWebSocketWithProtocol(url, protocols[0])
      }
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
            removeEventListeners()
          }
          "error" -> {
            continuation.resumeWithException(IllegalStateException("Apollo: WebSocket connection failed"))
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
}