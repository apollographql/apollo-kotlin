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
    val method: HttpMethod,
    val url: String,
    val headers: Map<String, String>,
    val body: HttpBody?,
) {
  fun copy(
      method: HttpMethod = this.method,
      url: String = this.url,
      headers: Map<String, String> = this.headers,
      body: HttpBody? = this.body,
  ): HttpRequest {
    return HttpRequest(
        method = method,
        url = url,
        headers = headers,
        body = body
    )
  }
}

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

fun HttpRequest.withHeader(name: String, value: String): HttpRequest {
  return HttpRequest(
      method = method,
      url = url,
      headers = headers + (name to value),
      body = body
  )
}