package com.apollographql.apollo3.mockserver

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.mapNotNull
import okio.Buffer
import okio.ByteString


private sealed interface Item
private class BytesItem(val bytes: ByteString): Item
private class DelayItem(val delayMillis: Long): Item

internal class MultipartBodyImpl(private val boundary: String, private val partsContentType: String) : MultipartBody {
  private var isFirst = true

  private val channel = Channel<Item>(Channel.UNLIMITED)

  internal fun consumeAsFlow(): Flow<ByteString> {
    return channel.consumeAsFlow().mapNotNull {
      when (it) {
        is DelayItem -> {
          delay(it.delayMillis)
          null
        }
        is BytesItem -> it.bytes
      }
    }.asChunked()
  }

  override fun enqueuePart(bytes: ByteString, isLast: Boolean) {

    val b = Buffer().apply {
      if (isFirst) {
        writeUtf8("--$boundary\r\n")
        isFirst = false
      }
      val endBoundary = if (isLast) "--$boundary--" else "--$boundary"

      writeUtf8("Content-Length: ${bytes.size}\r\n")
      writeUtf8("Content-Type: $partsContentType\r\n")
      writeUtf8("\r\n")
      write(bytes)
      writeUtf8("\r\n$endBoundary\r\n")
    }.readByteString()

    channel.trySend(BytesItem(b))
    if (isLast) {
      channel.close()
    }
  }

  override fun enqueueDelay(delayMillis: Long) {
    channel.trySend(DelayItem(delayMillis))
  }
}
