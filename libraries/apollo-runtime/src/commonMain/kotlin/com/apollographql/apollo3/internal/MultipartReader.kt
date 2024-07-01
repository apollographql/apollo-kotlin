/*
 * Copyright (C) 2020 Square, Inc.
 * Copyright (c) 2022 Apollo Graph, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apollographql.apollo.internal

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.exception.DefaultApolloException
import okio.Buffer
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8
import okio.Closeable
import okio.Options
import okio.Source
import okio.buffer

/**
 * This was copied from OkHttp and slightly modified to work in a Multiplatform project.
 *
 * Original source: https://github.com/square/okhttp/blob/parent-4.9.2/okhttp/src/main/kotlin/okhttp3/MultipartReader.kt
 *
 * Reads a stream of [RFC 2046][rfc_2046] multipart body parts. Callers read parts one-at-a-time
 * until [nextPart] returns null. After calling [nextPart] any preceding parts should not be read.
 *
 * Typical use loops over the parts in sequence:
 *
 * ```
 * val response: Response = call.execute()
 * val multipartReader = MultipartReader(response.body!!)
 *
 * multipartReader.use {
 *   while (true) {
 *     val part = multipartReader.nextPart() ?: break
 *     process(part.headers, part.body)
 *   }
 * }
 * ```
 *
 * Note that [nextPart] will skip any unprocessed data from the preceding part. If the preceding
 * part is particularly large or if the underlying source is particularly slow, the [nextPart] call
 * may be slow!
 *
 * Closing a part **does not** close this multipart reader; callers must explicitly close this with
 * [close].
 *
 * [rfc_2046]: http://www.ietf.org/rfc/rfc2046.txt
 */
@ApolloInternal
class MultipartReader constructor(
    private val source: BufferedSource,
    val boundary: String,
) : Closeable {
  /** This delimiter typically precedes the first part. */
  private val dashDashBoundary = Buffer()
      .writeUtf8("--")
      .writeUtf8(boundary)
      .readByteString()

  /**
   * This delimiter typically precedes all subsequent parts. It may also precede the first part
   * if the body contains a preamble.
   */
  private val crlfDashDashBoundary = Buffer()
      .writeUtf8("\r\n--")
      .writeUtf8(boundary)
      .readByteString()

  private var partCount = 0
  private var closed = false
  private var noMoreParts = false

  /** This is only part that's allowed to read from the underlying source. */
  private var currentPart: PartSource? = null

  fun nextPart(): Part? {
    check(!closed) { "closed" }

    if (noMoreParts) return null

    // Read a boundary, skipping the remainder of the preceding part as necessary.
    if (partCount == 0 && source.rangeEquals(0L, dashDashBoundary)) {
      // This is the first part. Consume "--" followed by the boundary.
      source.skip(dashDashBoundary.size.toLong())
    } else {
      // This is a subsequent part or a preamble. Skip until "\r\n--" followed by the boundary.
      while (true) {
        val toSkip = currentPartBytesRemaining(maxResult = 8192)
        if (toSkip == 0L) break
        source.skip(toSkip)
      }
      source.skip(crlfDashDashBoundary.size.toLong())
    }

    // Read either \r\n or --\r\n to determine if there is another part.
    var whitespace = false
    afterBoundaryLoop@ while (true) {
      when (source.select(afterBoundaryOptions)) {
        0 -> {
          // "\r\n--<boundary>--": More parts, immediately followed by the closing delimiter.
          if (partCount == 0) throw DefaultApolloException("expected at least 1 part")
          noMoreParts = true
          return null
        }

        1 -> {
          // "\r\n": We've found a new part.
          partCount++
          break@afterBoundaryLoop
        }

        2 -> {
          // "--": No more parts.
          if (whitespace) throw DefaultApolloException("unexpected characters after boundary")
          if (partCount == 0) throw DefaultApolloException("expected at least 1 part")
          noMoreParts = true
          return null
        }

        3, 4 -> {
          // " " or "\t" Ignore whitespace and keep looking.
          whitespace = true
          continue@afterBoundaryLoop
        }

        -1 -> {
          if (source.exhausted()) {
            throw DefaultApolloException("premature end of multipart body")
          } else {
            throw DefaultApolloException("unexpected characters after boundary")
          }
        }
      }
    }

    // There's another part. Parse its headers and return it.
    val headers = readHeaders(source)
    val partSource = PartSource()
    currentPart = partSource
    return Part(headers, partSource.buffer())
  }

  /** A single part in the stream. It is an error to read this after calling [nextPart]. */
  private inner class PartSource : Source {
    override fun close() {
      if (currentPart == this) {
        currentPart = null
      }
    }

    override fun read(sink: Buffer, byteCount: Long): Long {
      require(byteCount >= 0L) { "byteCount < 0: $byteCount" }
      check(currentPart == this) { "closed" }
      return when (val limit = currentPartBytesRemaining(maxResult = byteCount)) {
        0L -> -1L // No more bytes in this part.
        else -> source.read(sink, limit)
      }
    }

    override fun timeout() = source.timeout()
  }

  /**
   * Returns a value in [0..maxByteCount] with the number of bytes that can be read from [source] in
   * the current part. If this returns 0 the current part is exhausted; otherwise it has at least
   * one byte left to read.
   */
  private fun currentPartBytesRemaining(maxResult: Long): Long {
    source.require(crlfDashDashBoundary.size.toLong())

    return when (val delimiterIndex = source.buffer.indexOf(crlfDashDashBoundary)) {
      -1L -> minOf(maxResult, source.buffer.size - crlfDashDashBoundary.size + 1)
      else -> minOf(maxResult, delimiterIndex)
    }
  }

  /** These options follow the boundary. */
  private val afterBoundaryOptions = Options.of(
      // 0. More parts, immediately followed by the closing delimiter.
      // Not sure this is compliant, but it's been reported in the wild. See https://github.com/apollographql/apollo-kotlin/issues/4596
      "\r\n--$boundary--".encodeUtf8(),

      // 1. More parts.
      "\r\n".encodeUtf8(),

      // 2. No more parts.
      "--".encodeUtf8(),

      // 3. Optional whitespace. Only used if there are more parts.
      " ".encodeUtf8(),

      // 4. Optional whitespace. Only used if there are more parts.
      "\t".encodeUtf8(),
  )

  override fun close() {
    if (closed) return
    closed = true
    currentPart = null
    source.close()
  }

  /** A single part in a multipart body. */
  @ApolloInternal
  class Part(
      val headers: List<HttpHeader>,
      val body: BufferedSource,
  ) : Closeable by body

  private companion object {
    private fun readHeaders(source: BufferedSource): List<HttpHeader> {
      val headers = mutableListOf<HttpHeader>()
      while (true) {
        val line = source.readUtf8LineStrict()
        if (line.isEmpty()) break

        val index = line.indexOf(':')
        check(index != -1) { "Unexpected header: $line" }
        headers += HttpHeader(line.substring(0, index).trim(), line.substring(index + 1).trim())
      }
      return headers
    }
  }
}
