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

/**
 * Get the value of the "name" header. HTTP header names are case insensitive, see https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
 *
 * @param name: the name of the header
 */
fun List<HttpHeader>.valueOf(name: String): String? = firstOrNull { it.name.equals(name, true) }?.value

/**
 * a HTTP request to be sent
 */
class HttpRequest
@Deprecated("Please use HttpRequest.Builder methods instead.  This will be removed in v3.0.0.")
/* private */ constructor(
    val method: HttpMethod,
    val url: String,
    val headers: List<HttpHeader>,
    val body: HttpBody?,
) {

  fun newBuilder() = Builder(
      method = method,
      url = url,
  ).apply {
    if (body != null) body(body)
    addHeaders(headers)
  }

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

  class Builder(
      private val method: HttpMethod,
      private val url: String,
  ) {
    private var body: HttpBody? = null
    private val headers = mutableListOf<HttpHeader>()

    fun body(body: HttpBody) {
      this.body = body
    }

    fun addHeader(name: String, value: String) = apply {
      headers += HttpHeader(name, value)
    }

    fun addHeaders(headers: List<HttpHeader>) = apply {
      this.headers.addAll(headers)
    }

    fun build() = HttpRequest(
        method = method,
        url = url,
        headers = headers,
        body = body,
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
class HttpResponse
@Deprecated("Please use HttpResponse.Builder methods instead.  This will be removed in v3.0.0.")
/* private */ constructor(
    val statusCode: Int,
    val headers: List<HttpHeader>,
    /**
     * A streamable body.
     */
    private val bodySource: BufferedSource?,
    /**
     * An immutable body that can be freezed when used from Kotlin native.
     * Prefer [bodySource] on non-native so that the response can be streamed.
     */
    private val bodyString: ByteString?,
) {

  val body: BufferedSource?
    get() = bodySource ?: bodyString?.let { Buffer().write(it) }

  fun newBuilder() = Builder(
      statusCode = statusCode,
  ).apply {
    if (bodySource != null) body(bodySource)
    if (bodyString != null) body(bodyString)
    addHeaders(headers)
  }

  class Builder(
      val statusCode: Int,
  ) {
    private var bodySource: BufferedSource? = null
    private var bodyString: ByteString? = null
    private val headers = mutableListOf<HttpHeader>()

    /**
     * A streamable body.
     */
    fun body(bodySource: BufferedSource) = apply {
      check(bodyString == null) { "body(ByteString) was already set, either body(BufferedSource) or body(ByteString) can be set" }
      this.bodySource = bodySource
    }

    /**
     * An immutable body that can be freezed when used from Kotlin native.
     * Prefer [bodySource] on non-native so that the response can be streamed.
     */
    fun body(bodyString: ByteString) = apply {
      check(bodySource == null) { "body(BufferedSource) was already set, either body(BufferedSource) or body(ByteString) can be set" }
      this.bodyString = bodyString
    }

    fun addHeader(name: String, value: String) = apply {
      headers += HttpHeader(name, value)
    }

    fun addHeaders(headers: List<HttpHeader>) = apply {
      this.headers.addAll(headers)
    }

    fun build(): HttpResponse {
      return HttpResponse(
          statusCode = statusCode,
          headers = headers,
          bodySource = bodySource,
          bodyString = bodyString,
      )
    }
  }
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

@Deprecated("Please use HttpRequest.Builder methods instead.  This will be removed in v3.0.0.")
fun HttpRequest.withHeader(name: String, value: String) = newBuilder().addHeader(name, value).build()

@Deprecated("Please use HttpRequest.Builder methods instead.  This will be removed in v3.0.0.")
fun HttpRequest.withHeaders(headers: List<HttpHeader>) = newBuilder().addHeaders(headers).build()

@Deprecated("Please use HttpResponse.Builder methods instead.  This will be removed in v3.0.0.")
fun HttpResponse.withHeaders(headers: List<HttpHeader>) = newBuilder().addHeaders(headers).build()
