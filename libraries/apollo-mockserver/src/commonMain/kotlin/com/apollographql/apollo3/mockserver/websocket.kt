package com.apollographql.apollo3.mockserver

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.toByteString
import okio.IOException

internal suspend fun readFrames(reader: Reader, onMessage: (WebSocketMessage) -> Unit) {
  val currentMessage = Buffer()
  var currentOpcode: Int? = null

  suspend fun require(size: Long) {
    while (reader.buffer.size < size) {
      reader.fillBuffer()
    }
  }

  while (true) {
    /**
     * Check if the client closed the connection
     */
    if (reader.buffer.size == 0L) {
      try {
        reader.fillBuffer()
      } catch (e: IOException) {
        throw ConnectionClosed(e)
      }
    }

    require(2)

    var b = reader.buffer.readByte().toInt()

    val fin = b.and(0x80) != 0
    var opcode = b.and(0xf)

    b = reader.buffer.readByte().toInt()
    val mask = b.and(0x80) != 0

    val payloadLength = when (val b2 = b.and(0x7f)) {
      127 -> {
        require(8)
        reader.buffer.readLong()
      }

      126 -> {
        require(2)
        reader.buffer.readShort().toLong().and(0xffff)
      }

      else -> b2.toLong()
    }

    val maskingKey = if (mask) {
      require(4)
      reader.buffer.readByteArray(4).map {
        it.toInt().and(0xff)
      }
    } else {
      null
    }

    check(payloadLength >= 0 && payloadLength < Int.MAX_VALUE) {
      "Payload length too long: $payloadLength"
    }
    require(payloadLength)

    val payload = Buffer()
    if (maskingKey == null) {
      reader.buffer.read(payload, payloadLength)
    } else {
      for (i in 0.until(payloadLength.toInt())) {
        payload.writeByte(reader.buffer.readByte().toInt().xor(maskingKey[i % 4]))
      }
    }

    if (opcode == OPCODE_CONTINUATION) {
      opcode = currentOpcode ?: error("")
    }

    when (opcode) {
      OPCODE_CLOSE -> {
        var code: Int? = null
        var reason: String? = null
        if (payloadLength > 0) {
          check(payloadLength >= 2)
          code = payload.readShort().toUShort().toInt()
          if (payloadLength > 2) {
            reason = payload.readUtf8(payloadLength - 2)
          }
        }

        onMessage(CloseFrame(code, reason))
        break
      }

      OPCODE_PING -> {
        onMessage(PingFrame)
      }

      OPCODE_PONG -> {
        onMessage(PongFrame)
      }

      OPCODE_TEXT -> {
        currentMessage.write(payload, payloadLength)
        if (fin) {
          onMessage(TextMessage(currentMessage.readUtf8()))
          currentOpcode = null
        } else {
          currentOpcode = opcode
        }
      }

      OPCODE_BINARY -> {
        currentMessage.write(payload, payloadLength)
        if (fin) {
          onMessage(DataMessage(currentMessage.readByteArray()))
          currentOpcode = null
        } else {
          currentOpcode = opcode
        }
      }
    }
  }
}

internal fun pongFrame(): ByteArray {
  val buffer = Buffer()
  // FIN + opcode
  buffer.writeByte(0x80 + OPCODE_PONG)
  // No masking, no payload
  buffer.writeByte(0)

  return buffer.readByteArray()
}

internal fun pingFrame(): ByteArray {
  val buffer = Buffer()
  // FIN + opcode
  buffer.writeByte(0x80 + OPCODE_PING)
  // No masking, no payload
  buffer.writeByte(0)

  return buffer.readByteArray()
}

private fun closeFrame(code: Int?, reason: String?): ByteString {
  val buffer = Buffer()
  // FIN + opcode
  buffer.writeByte(0x80 + OPCODE_CLOSE)

  val payload = Buffer()
  if (code != null) {
    payload.writeShort(code)
  }
  if (reason != null) {
    payload.writeUtf8(reason)
  }

  buffer.writePayloadLength(false, payload.size)

  buffer.writeAll(payload)

  return buffer.readByteString()
}

private fun textFrame(text: String): ByteString {
  val buffer = Buffer()
  // FIN + opcode
  buffer.writeByte(0x80 + OPCODE_TEXT)

  val payload = Buffer()
  payload.writeUtf8(text)

  buffer.writePayloadLength(false, payload.size)

  buffer.writeAll(payload)

  return buffer.readByteString()
}

private fun binaryFrame(data: ByteArray): ByteString {
  val buffer = Buffer()
  // FIN + opcode
  buffer.writeByte(0x80 + OPCODE_BINARY)

  val payload = Buffer()
  payload.write(data)

  buffer.writePayloadLength(false, payload.size)

  buffer.writeAll(payload)

  return buffer.readByteString()
}

private fun Buffer.writePayloadLength(mask: Boolean, size: Long) {
  check (size >= 0 && size < Int.MAX_VALUE)

  val maskByte = if (mask) 1.shl(7) else 0
  when {
    size <= 125 -> {
      writeByte(maskByte + size.toInt())
    }
    size.toUShort() <= UShort.MAX_VALUE -> {
      writeByte(maskByte + 126)
      writeShort(size.toInt())
    }
    else -> {
      writeByte(maskByte + 127)
      writeLong(size)
    }
  }

}

internal interface BodyItem
internal class MessageItem(val message: WebSocketMessage): BodyItem
internal object CloseItem: BodyItem

internal class WebSocketBodyImpl: WebSocketBody {
  private val channel = Channel<BodyItem>(Channel.UNLIMITED)

  internal fun consumeAsFlow(): Flow<ByteString> {
    return channel.consumeAsFlow().takeWhile {
      it is MessageItem
    }.map { (it as MessageItem).message.toFrame() }
  }

  override fun enqueueMessage(message: WebSocketMessage) {
    channel.trySend(MessageItem(message))
  }

  override fun close() {
    channel.trySend(CloseItem)
  }
}

private fun WebSocketMessage.toFrame(): ByteString {
  return when (this) {
    is PongFrame -> pongFrame().toByteString()
    is PingFrame -> pingFrame().toByteString()
    is CloseFrame -> closeFrame(code, reason)
    is TextMessage -> textFrame(text)
    is DataMessage -> binaryFrame(data)
  }
}

internal fun MockResponse.replaceWebSocketHeaders(request: MockRequestBase): MockResponse {
  return newBuilder()
      .headers(headers.entries.mapNotNull {
        if (it.key.lowercase() == "sec-websocket-accept") {
          check(it.value == "APOLLO_REPLACE_ME")
          it.key to webSocketAccept(request)
        } else if (it.key.lowercase() == "sec-websocket-protocol" && it.value == "APOLLO_REPLACE_ME") {
          webSocketProtocol(request)?.let {
            "sec-websocket-protocol" to it
          }
        } else {
          it.key to it.value
        }
      }.toMap())
      .build()
}

internal fun webSocketAccept(request: MockRequestBase): String {
  // See https://www.rfc-editor.org/rfc/rfc6455#section-1.3
  val guid = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
  val key = request.headers.entries.first { it.key.lowercase() == "sec-websocket-key" }.value

  val buffer = Buffer()

  buffer.writeUtf8(key)
  buffer.writeUtf8(guid)

  return buffer.sha1().base64()
}


internal fun webSocketProtocol(request: MockRequestBase): String? {
  return request.headers.entries.firstOrNull { it.key.lowercase() == "sec-websocket-protocol" }?.value
      ?.split(",")
      ?.map { it.trim() }
      ?.firstOrNull()
}

private const val OPCODE_CONTINUATION = 0x0
private const val OPCODE_TEXT = 0x1
private const val OPCODE_BINARY = 0x2
private const val OPCODE_PONG = 0xa
private const val OPCODE_PING = 0x9
private const val OPCODE_CLOSE = 0x8
