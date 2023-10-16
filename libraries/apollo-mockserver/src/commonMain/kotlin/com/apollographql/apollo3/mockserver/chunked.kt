package com.apollographql.apollo3.mockserver

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow
import okio.Buffer
import okio.BufferedSource
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8

/**
 * Read a source encoded in the "Transfer-Encoding: chunked" encoding.
 * This format is a sequence of:
 * - chunk-size (in hexadecimal) + CRLF
 * - chunk-data + CRLF
 */
internal fun BufferedSource.readChunked(buffer: Buffer) {
  while (true) {
    val line = readUtf8Line()
    if (line.isNullOrBlank()) break

    val chunkSize = line.toLong(16)
    if (chunkSize == 0L) break

    read(buffer, chunkSize)
    readUtf8Line() // CRLF
  }
}

/**
 * Turns a Flow<ByteString> into a `Transfer-Encoding: chunked` compatible one.
 */
internal fun Flow<ByteString>.asChunked(): Flow<ByteString> {
  // Chunked format is a sequence of:
  // - chunk-size (in hexadecimal) + CRLF
  // - chunk-data + CRLF
  // Ended with a chunk-size of 0 + CRLF + CRLF
  return map { payload ->
    val buffer = Buffer().apply {
      writeHexadecimalUnsignedLong(payload.size.toLong())
      writeUtf8("\r\n")
      write(payload)
      writeUtf8("\r\n")
    }
    buffer.readByteString()
  }
      .onCompletion { emit("0\r\n\r\n".encodeUtf8()) }
}

internal fun createMultipartMixedChunkedResponse(
    parts: List<String>,
    statusCode: Int = 200,
    partsContentType: String = "application/json; charset=utf-8",
    headers: Map<String, String> = emptyMap(),
    responseDelayMillis: Long = 0,
    chunksDelayMillis: Long = 0,
    boundary: String = "-",
): MockResponse {
  return ChunkedResponse(
      statusCode = statusCode,
      partsContentType = partsContentType,
      headers = headers,
      responseDelayMillis = responseDelayMillis,
      chunksDelayMillis = chunksDelayMillis,
      boundary = boundary,
  ).apply {
    parts.withIndex().forEach { (index, part) ->
      send(content = part, isFirst = index == 0, isLast = index == parts.lastIndex)
    }
  }.response
}

private class ChunkedResponse(
    statusCode: Int = 200,
    private val partsContentType: String = "application/json; charset=utf-8",
    headers: Map<String, String> = emptyMap(),
    responseDelayMillis: Long = 0,
    chunksDelayMillis: Long = 0,
    private val boundary: String = "-",
) {
  private val chunkChannel = Channel<ByteString>(Channel.UNLIMITED)

  val response = MockResponse.Builder()
      .statusCode(statusCode)
      .body(
          chunkChannel.receiveAsFlow().map {
            delay(chunksDelayMillis)
            it
          }.asChunked()
      )
      .headers(
          headers + mapOf(
              "Content-Type" to """multipart/mixed; boundary="$boundary"""",
              "Transfer-Encoding" to "chunked",
          )
      )
      .delayMillis(responseDelayMillis)
      .build()

  fun send(
      content: String,
      isFirst: Boolean = false,
      isLast: Boolean = false,
  ) {
    chunkChannel.trySend(createMultipartMixedPart(
        content = content,
        isFirst = isFirst,
        isLast = isLast,
        contentType = partsContentType,
        boundary = boundary,
    ).encodeUtf8())
    if (isLast) chunkChannel.close()
  }
}


internal fun MockServer.enqueueMultipart(
    parts: List<String>,
    statusCode: Int = 200,
    partsContentType: String = "application/json; charset=utf-8",
    headers: Map<String, String> = emptyMap(),
    responseDelayMillis: Long = 0,
    chunksDelayMillis: Long = 0,
    boundary: String = "-",
) {
  enqueue(createMultipartMixedChunkedResponse(
      parts = parts,
      statusCode = statusCode,
      partsContentType = partsContentType,
      headers = headers,
      responseDelayMillis = responseDelayMillis,
      chunksDelayMillis = chunksDelayMillis,
      boundary = boundary
  ))
}

internal fun createMultipartMixedPart(
    content: String,
    contentType: String = "application/json; charset=utf-8",
    boundary: String = "-",
    isFirst: Boolean = false,
    isLast: Boolean = false,
): String {
  val startBoundary = if (isFirst) "--$boundary\r\n" else ""
  val contentLengthHeader = "Content-Length: ${content.length}"
  val contentTypeHeader = "Content-Type: $contentType"
  val endBoundary = if (isLast) "--$boundary--" else "--$boundary"
  return startBoundary +
      "$contentLengthHeader\r\n" +
      "$contentTypeHeader\r\n" +
      "\r\n" +
      "$content\r\n" +
      "$endBoundary\r\n"
}
