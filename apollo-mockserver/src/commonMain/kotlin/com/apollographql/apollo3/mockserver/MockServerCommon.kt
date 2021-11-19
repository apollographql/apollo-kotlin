package com.apollographql.apollo3.mockserver

import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

fun parseHeader(line: String): Pair<String, String> {
  val index = line.indexOfFirst { it == ':' }
  check (index >= 0) {
    "Invalid header: $line"
  }

  return line.substring(0, index).trim() to line.substring(index + 1, line.length).trim()
}

class MockRecordedRequest(
    val method: String,
    val path: String,
    val version: String,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteString = ByteString.EMPTY
)

fun writeResponse(sink: BufferedSink, mockResponse: MockResponse, version: String) {
  sink.writeUtf8("${version} ${mockResponse.statusCode}\r\n")
  val contentLengthHeader = mapOf("Content-Length" to mockResponse.body.size.toString())

  (contentLengthHeader + mockResponse.headers).forEach {
    sink.writeUtf8("${it.key}: ${it.value}\r\n")
  }
  sink.writeUtf8("\r\n")
  sink.flush()

  if (mockResponse.body.size > 0) {
    sink.write(mockResponse.body)
  }
  sink.flush()
}

class MockResponse(
    val statusCode: Int = 200,
    val body: ByteString = ByteString.EMPTY,
    val headers: Map<String, String> = emptyMap(),
    val delayMillis: Long = 0,
) {
  @JvmOverloads
  constructor(
      body: String,
      statusCode: Int = 200,
      headers: Map<String, String> = emptyMap()
  ) : this(statusCode, body.encodeUtf8(), headers)
}

internal fun readRequest(source: BufferedSource): MockRecordedRequest? {
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
  while(true) {
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
  check(transferEncoding == null || transferEncoding == "identity") {
    "Transfer-Encoding $transferEncoding is not supported"
  }

  val buffer = Buffer()
  if (contentLength > 0) {
    source.read(buffer, contentLength)
  }

  return MockRecordedRequest(
      method = method,
      path = path,
      version = version,
      headers = headers,
      body = buffer.readByteString()
  )
}

fun parseRequestLine(line: String): Triple<String, String, String> {
  val regex = Regex("([A-Z-a-z]*) ([^ ]*) (.*)")
  val match = regex.matchEntire(line)
  check (match != null) {
    "Cannot match request line: $line"
  }

  val method = match.groupValues[1].uppercase()
  check (method in listOf("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH")) {
    "Unkown method $method"
  }

  return Triple(method, match.groupValues[2], match.groupValues[3])
}