package com.apollographql.apollo3.mockserver

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.mapNotNull
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8


private sealed interface Item
private class BytesItem(val bytes: ByteString): Item
private class DelayItem(val delayMillis: Long): Item

private fun String.toBytesItem(): Item = BytesItem(encodeUtf8())

internal class MultipartBodyImpl(private val boundary: String, private val partsContentType: String) : MultipartBody {
  private var isFirst = false

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

  override fun enqueuePart(bytes: ByteString) {
    if (isFirst) {
      channel.trySend("--$boundary\r\n".toBytesItem())
    }
    val headers = """
      "Content-Length: ${bytes.size}\r\n" +
      "Content-Type: $partsContentType\r\n" +
      "\r\n"
    """.trimIndent()

    channel.trySend(headers.toBytesItem())
    channel.trySend("--$boundary\r\n".toBytesItem())
  }

  override fun enqueueDelay(delayMillis: Long) {
    channel.trySend(DelayItem(delayMillis))
  }

  override fun close() {
    channel.trySend("--".toBytesItem())
  }

}
