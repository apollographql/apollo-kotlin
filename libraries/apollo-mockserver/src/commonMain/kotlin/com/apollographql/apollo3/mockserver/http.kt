package com.apollographql.apollo3.mockserver

import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource

private fun parseHeader(line: String): Pair<String, String> {
  val index = line.indexOfFirst { it == ':' }
  check(index >= 0) {
    "Invalid header: $line"
  }

  return line.substring(0, index).trim() to line.substring(index + 1, line.length).trim()
}

internal suspend fun writeResponse(sink: BufferedSink, mockResponse: MockResponse, version: String) {
  sink.writeUtf8("$version ${mockResponse.statusCode}\r\n")
  // We don't support 'Connection: Keep-Alive', so indicate it to the client
  val headers = mockResponse.headers + mapOf("Connection" to "close")
  headers.forEach {
    sink.writeUtf8("${it.key}: ${it.value}\r\n")
  }
  sink.writeUtf8("\r\n")
  sink.flush()

  mockResponse.body.collect {
    sink.write(it)
    sink.flush()
  }
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
