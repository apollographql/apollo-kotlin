package com.apollographql.apollo3.mockserver

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
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