package com.apollographql.apollo3.network.ws

import com.apollographql.apollo3.internal.ChannelWrapper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import okio.ByteString
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket
import org.w3c.files.Blob

actual class DefaultWebSocketEngine : WebSocketEngine {
  override suspend fun open(
      url: String,
      headers: Map<String, String>,
  ): WebSocketConnection {
    val messageChannel = ChannelWrapper(Channel<String>(Channel.UNLIMITED))
    val socket = WebSocket(url)
    val webSocketOpenResult = CompletableDeferred<Unit>()
    socket.onopen = { webSocketOpenResult.complete(Unit) }
    socket.onmessage = { messageEvent: MessageEvent ->
      val data = messageEvent.data
      if (data is String) {
        messageChannel.trySend(data)
      }
      if (data is ByteString) {
        messageChannel.trySend(data.utf8())
      }
    }

    webSocketOpenResult.await()

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
      }
    }
  }
}

