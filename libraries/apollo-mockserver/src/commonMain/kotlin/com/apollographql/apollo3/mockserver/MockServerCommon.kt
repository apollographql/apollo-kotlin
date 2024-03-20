package com.apollographql.apollo3.mockserver

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_3_1
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_8_3
import com.apollographql.apollo3.annotations.ApolloInternal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import kotlin.jvm.JvmOverloads

@Deprecated("This shouldn't be part of the public API and will be removed in Apollo Kotlin 4. If you needed this, please open an issue.")
@ApolloDeprecatedSince(v3_8_3)
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

@Deprecated("This shouldn't be part of the public API and will be removed in Apollo Kotlin 4. If you needed this, please open an issue.")
@ApolloDeprecatedSince(v3_8_3)
suspend fun writeResponse(sink: BufferedSink, mockResponse: MockResponse, version: String) {
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

class MockResponse
@Deprecated("Use MockResponse.Builder instead", ReplaceWith("MockResponse.Builder().statusCode(statusCode).headers(headers).body(body).delayMillis(delayMillis).build()"))
@ApolloDeprecatedSince(v3_3_1)
constructor(
    val statusCode: Int = 200,
    val body: Flow<ByteString> = emptyFlow(),
    val headers: Map<String, String> = mapOf("Content-Length" to "0"),
    val delayMillis: Long = 0,
) {
  @Deprecated("Use MockResponse.Builder instead", ReplaceWith("MockResponse.Builder().statusCode(statusCode).headers(headers).body(body).delayMillis(delayMillis).build()"))
  @ApolloDeprecatedSince(v3_3_1)
  @Suppress("DEPRECATION")
  @JvmOverloads
  constructor(
      body: String,
      statusCode: Int = 200,
      headers: Map<String, String> = emptyMap(),
      delayMillis: Long = 0,
  ) : this(
      statusCode = statusCode,
      body = flowOf(body.encodeUtf8()),
      headers = headers + mapOf("Content-Length" to body.encodeUtf8().size.toString()),
      delayMillis = delayMillis,
  )

  @Deprecated("Use MockResponse.Builder instead", ReplaceWith("MockResponse.Builder().statusCode(statusCode).body(body).headers(headers).delayMillis(delayMillis).build()"))
  @ApolloDeprecatedSince(v3_3_1)
  @Suppress("DEPRECATION")
  constructor(
      body: ByteString,
      statusCode: Int = 200,
      headers: Map<String, String> = emptyMap(),
      delayMillis: Long = 0,
  ) : this(
      statusCode = statusCode,
      body = flowOf(body),
      headers = headers + mapOf("Content-Length" to body.size.toString()),
      delayMillis = delayMillis,
  )

  class Builder {
    private var statusCode: Int = 200
    private var body: Flow<ByteString> = emptyFlow()
    private val headers = mutableMapOf<String, String>()
    private var delayMillis: Long = 0
    private var contentLength: Int? = null

    fun statusCode(statusCode: Int) = apply { this.statusCode = statusCode }

    fun body(body: Flow<ByteString>) = apply { this.body = body }

    fun body(body: ByteString) = apply {
      this.body = flowOf(body)
      contentLength = body.size
    }

    fun body(body: String) = body(body.encodeUtf8())

    fun headers(headers: Map<String, String>) = apply {
      this.headers.clear()
      this.headers += headers
    }

    fun addHeader(key: String, value: String) = apply { headers[key] = value }

    fun delayMillis(delayMillis: Long) = apply { this.delayMillis = delayMillis }

    fun build(): MockResponse {
      val headersWithContentLength = if (contentLength == null) headers else headers + mapOf("Content-Length" to contentLength.toString())
      @Suppress("DEPRECATION")
      return MockResponse(statusCode = statusCode, body = body, headers = headersWithContentLength, delayMillis = delayMillis)
    }
  }
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

  val (method, path, version) = @Suppress("DEPRECATION") parseRequestLine(line)

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

    val (key, value) = @Suppress("DEPRECATION") parseHeader(line)
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
@ApolloInternal
fun BufferedSource.readChunked(buffer: Buffer) {
  while (true) {
    val line = readUtf8Line()
    if (line.isNullOrBlank()) break

    val chunkSize = line.toLong(16)
    if (chunkSize == 0L) break

    read(buffer, chunkSize)
    readUtf8Line() // CRLF
  }
}

@Deprecated("This shouldn't be part of the public API and will be removed in Apollo Kotlin 4. If you needed this, please open an issue.")
@ApolloDeprecatedSince(v3_8_3)
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
