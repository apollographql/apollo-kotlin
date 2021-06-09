package com.apollographql.apollo3.api.http

import okio.BufferedSink
import okio.BufferedSource
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8

enum class HttpMethod {
  Get, Post
}

interface HttpBody {
  val contentType: String
  val contentLength: Long

  /**
   * This can be called several times
   */
  fun writeTo(bufferedSink: BufferedSink)
}

class HttpRequest(
    val url: String,
    val headers: Map<String, String>,
    val method: HttpMethod,
    val body: HttpBody?,
)

class HttpResponse(
    val statusCode: Int,
    val headers: Map<String, String>,
    /**
     * A streamable body.
     */
    private val bodySource: BufferedSource?,
    /**
     * An immutable body for the native case where this class is frozen
     */
    private val bodyString: ByteString?,
) {
  val body: BufferedSource?
    get() = bodySource ?: bodyString?.let { Buffer().write(it) }
}

fun HttpBody(
    contentType: String,
    byteString: ByteString,
) = object : HttpBody {
  override val contentType
    get() = contentType
  override val contentLength
    get() = byteString.size.toLong()

  override fun writeTo(bufferedSink: BufferedSink) {
    bufferedSink.write(byteString)
  }
}

fun HttpBody(
    contentType: String,
    string: String,
): HttpBody = HttpBody(contentType, string.encodeUtf8())

