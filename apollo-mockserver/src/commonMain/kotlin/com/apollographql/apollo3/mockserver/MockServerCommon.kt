package com.apollographql.apollo3.mockserver

import kotlinx.coroutines.delay
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import kotlin.jvm.JvmOverloads

fun parseHeader(line: String): Pair<String, String> {
  val index = line.indexOfFirst { it == ':' }
  check(index >= 0) {
    "Invalid header: $line"
  }

  return line.substring(0, index).trim() to line.substring(index + 1, line.length).trim()
}

class MockRequest(
    val method: String,
    val path: String,
    val version: String,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteString = ByteString.EMPTY,
)

suspend fun writeResponse(sink: BufferedSink, mockResponse: MockResponse, version: String) {
  sink.writeUtf8("$version ${mockResponse.statusCode}\r\n")
  val isChunked = mockResponse.chunks.isNotEmpty()

  val headers = mockResponse.headers +
      if (isChunked) {
        mapOf("Transfer-Encoding" to "chunked")
      } else {
        mapOf("Content-Length" to mockResponse.body.size.toString())
      } +
      // We don't support 'Connection: Keep-Alive', so indicate it to the client
      mapOf("Connection" to "close")

  headers.forEach {
    sink.writeUtf8("${it.key}: ${it.value}\r\n")
  }
  sink.writeUtf8("\r\n")
  sink.flush()

  if (isChunked) {
    // Chunked format is a sequence of:
    // - chunk-size (in hexadecimal) + CRLF
    // - chunk-data + CRLF
    // Ended with a chunk-size of 0 + CRLF + CRLF
    for (chunk in mockResponse.chunks) {
      delay(chunk.delayMillis)
      sink.writeHexadecimalUnsignedLong(chunk.body.size.toLong())
      sink.writeUtf8("\r\n")
      sink.write(chunk.body)
      sink.writeUtf8("\r\n")
      sink.flush()
    }
    sink.writeUtf8("0\r\n\r\n")
    sink.flush()
  } else if (mockResponse.body.size > 0) {
    sink.write(mockResponse.body)
    sink.flush()
  }
}

class MockResponse(
    val statusCode: Int = 200,
    val body: ByteString = ByteString.EMPTY,
    val chunks: List<MockChunk> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
    val delayMillis: Long = 0,
) {
  @JvmOverloads
  constructor(
      body: String,
      chunks: List<MockChunk> = emptyList(),
      statusCode: Int = 200,
      headers: Map<String, String> = emptyMap(),
      delayMillis: Long = 0,
  ) : this(statusCode, body.encodeUtf8(), chunks, headers, delayMillis)
}

class MockChunk(
    val body: ByteString = ByteString.EMPTY,
    val delayMillis: Long = 0,
) {
  @JvmOverloads
  constructor(
      body: String,
      delayMillis: Long = 0,
  ) : this(body.encodeUtf8(), delayMillis)
}

interface MockServerHandler {
  /**
   * Handles the given [MockRequest].
   *
   * This method is called from one or several background threads and must be thread-safe.
   */
  fun handle(request: MockRequest): MockResponse
}

internal fun readRequest(source: BufferedSource): MockRequest? {
  var line = source.readUtf8Line()
  if (line == null) {
    // the connection was closed
    return null
  }

  val (method, path, version) = parseRequestLine(line)

  val headers = mutableMapOf<String, String>()
  /**
   * Read headers
   */
  while (true) {
    line = source.readUtf8Line()
    //println("Header Line: $line")
    if (line.isNullOrBlank()) {
      break
    }

    val (key, value) = parseHeader(line)
    headers.put(key, value)
  }

  val contentLength = headers["Content-Length"]?.toLongOrNull() ?: 0
  val transferEncoding = headers["Transfer-Encoding"]?.lowercase()
  check(transferEncoding == null || transferEncoding == "identity" || transferEncoding == "chunked") {
    "Transfer-Encoding $transferEncoding is not supported"
  }

  val buffer = Buffer()
  if (contentLength > 0) {
    source.read(buffer, contentLength)
  } else if (transferEncoding == "chunked") {
    source.readChunked(buffer)
  }

  return MockRequest(
      method = method,
      path = path,
      version = version,
      headers = headers,
      body = buffer.readByteString()
  )
}

/**
 * Read a source encoded in the "Transfer-Encoding: chunked" encoding.
 * This format is a sequence of:
 * - chunk-size (in hexadecimal) + CRLF
 * - chunk-data + CRLF
 */
private fun BufferedSource.readChunked(buffer: Buffer) {
  while (true) {
    val line = readUtf8Line()
    if (line.isNullOrBlank()) break

    val chunkSize = line.toLong(16)
    if (chunkSize == 0L) break

    read(buffer, chunkSize)
    readUtf8Line() // CRLF
  }
}

fun parseRequestLine(line: String): Triple<String, String, String> {
  val regex = Regex("([A-Z-a-z]*) ([^ ]*) (.*)")
  val match = regex.matchEntire(line)
  check(match != null) {
    "Cannot match request line: $line"
  }

  val method = match.groupValues[1].uppercase()
  check(method in listOf("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH")) {
    "Unkown method $method"
  }

  return Triple(method, match.groupValues[2], match.groupValues[3])
}
