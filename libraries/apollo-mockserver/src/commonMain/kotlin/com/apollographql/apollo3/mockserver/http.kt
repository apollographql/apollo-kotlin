package com.apollographql.apollo3.mockserver

import okio.Buffer

internal interface Reader {
  val buffer: Buffer
  suspend fun fillBuffer()
}

internal typealias WriteData = (ByteArray) -> Unit

private fun parseHeader(line: String): Pair<String, String> {
  val index = line.indexOfFirst { it == ':' }
  check(index >= 0) {
    "Invalid header: $line"
  }

  return line.substring(0, index).trim() to line.substring(index + 1, line.length).trim()
}


private fun parseRequestLine(line: String): Triple<String, String, String> {
  val regex = Regex("([A-Z-a-z]*) ([^ ]*) (.*)")
  val match = regex.matchEntire(line)
  check(match != null) {
    "Cannot match request line: $line"
  }

  val method = match.groupValues[1].uppercase()
  check(method in listOf("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH")) {
    "Unknown method $method"
  }

  return Triple(method, match.groupValues[2], match.groupValues[3])
}

internal class ConnectionClosed(cause: Throwable?) : Exception("client closed the connection", cause)

internal suspend fun readRequest(reader: Reader): MockRequestBase {
  suspend fun nextLine(): String {
    while (true) {
      val newline = reader.buffer.indexOf('\n'.code.toByte())
      if (newline != -1L) {
        return reader.buffer.readUtf8(newline + 1)
      } else {
        reader.fillBuffer()
      }
    }
  }

  suspend fun readBytes(size: Long): Buffer {
    val buffer2 = Buffer()
    while (buffer2.size < size) {
      if (reader.buffer.size == 0L) {
        reader.fillBuffer()
      }

      buffer2.write(reader.buffer, minOf(size, reader.buffer.size))
    }

    return buffer2
  }

  var line = try {
    nextLine()
  } catch (e: Exception) {
    /**
     * XXX: if the connection is closed in the middle of the first request line, this is detected
     * as a normal connection close.
     */
    throw ConnectionClosed(e)
  }

  val (method, path, version) = parseRequestLine(line.trimEol())
  //println("Line: ${line.trimEol()}")

  val headers = mutableMapOf<String, String>()

  /**
   * Read headers
   */
  while (true) {
    line = nextLine()
    //println("Headers: ${line.trimEol()}")
    if (line == "\r\n") {
      break
    }

    val (key, value) = parseHeader(line.trimEol())
    headers.put(key, value)
  }

  val contentLength = headers.headerValueOf("content-length")?.toLongOrNull() ?: 0
  val transferEncoding = headers.headerValueOf("transfer-encoding")?.lowercase()
  check(transferEncoding == null || transferEncoding == "identity" || transferEncoding == "chunked") {
    "Transfer-Encoding $transferEncoding is not supported"
  }

  val body = when {
    headers.get("Upgrade") == "websocket" -> null
    contentLength > 0 -> readBytes(contentLength)
    transferEncoding == "chunked" -> {
      val buffer2 = Buffer()
      /**
       * Read a source encoded in the "Transfer-Encoding: chunked" encoding.
       * This format is a sequence of:
       * - chunk-size (in hexadecimal) + CRLF
       * - chunk-data + CRLF
       */
      while (true) {
        val chunkSize = nextLine().trimEol().toLong(16)
        if (chunkSize == 0L) {
          check(nextLine() == "\r\n") // CRLF
          break
        } else {
          buffer2.writeAll(readBytes(chunkSize))
          check(nextLine() == "\r\n") // CRLF
        }
      }
      buffer2
    }

    else -> Buffer()
  }


  return if (body != null) {
    MockRequest(
        method = method,
        path = path,
        version = version,
        headers = headers,
        body = body.readByteString()
    )
  } else {
    WebsocketMockRequest(
        method = method,
        path = path,
        version = version,
        headers = headers,
    )
  }
}

private fun String.trimEol() = this.trimEnd('\r', '\n')

internal suspend fun writeResponse(response: MockResponse, version: String, writeData: WriteData) {
  writeData("$version ${response.statusCode}\r\n".encodeToByteArray())

  val headers = response.headers
  headers.forEach {
    writeData("${it.key}: ${it.value}\r\n".encodeToByteArray())
  }
  writeData("\r\n".encodeToByteArray())

  response.body.collect {
    // XXX: flow control
    writeData(it.toByteArray())
  }
}