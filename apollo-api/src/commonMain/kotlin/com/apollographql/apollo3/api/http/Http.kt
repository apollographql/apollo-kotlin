package com.apollographql.apollo3.api.http

import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
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

/**
 * a HTTP header
 */
class HttpHeader(val name: String, val value: String)

fun List<HttpHeader>.valueOf(name: String): String? = firstOrNull { it.name == name }?.value

/**
 * a HTTP request to be sent
 */
class HttpRequest(
    val method: HttpMethod,
    val url: String,
    val headers: List<HttpHeader>,
    val body: HttpBody?,
) {
  fun copy(
      method: HttpMethod = this.method,
      url: String = this.url,
      headers: List<HttpHeader> = this.headers,
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

/**
 * an HTTP Response.
 *
 * Specifying both [bodySource] and [bodySource] is invalid
 *
 * The [body] of a [HttpResponse] must always be closed if non null
 */
class HttpResponse(
    val statusCode: Int,
    val headers: List<HttpHeader>,
    /**
     * A streamable body.
     */
    private val bodySource: BufferedSource?,
    /**
     * An immutable body that can be freezed when used from Kotlin native.
     * Prefer [bodySource] on the JVM so that the response can be streamed.
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

/**
 * Creates a new [HttpBody] from a [String]
 */
fun HttpBody(
    contentType: String,
    string: String,
): HttpBody = HttpBody(contentType, string.encodeUtf8())

/**
 * adds a header to a given [HttpRequest]
 */
fun HttpRequest.withHeader(name: String, value: String): HttpRequest {
  return HttpRequest(
      method = method,
      url = url,
      headers = headers + HttpHeader(name, value),
      body = body
  )
}

/**
 * adds multiple headers to a given [HttpRequest]
 */
fun HttpRequest.withHeaders(headers: List<HttpHeader>): HttpRequest {
  return HttpRequest(
      method = method,
      url = url,
      headers = this.headers + headers,
      body = body
  )
}

/**
 * adds multiple headers to a given [HttpResponse]
 */
fun HttpResponse.withHeaders(headers: List<HttpHeader>): HttpResponse {
  return HttpResponse(
      statusCode = statusCode,
      headers = this.headers + headers,
      bodySource = body,
      bodyString = null
  )
}