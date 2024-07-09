package com.apollographql.apollo.api.http

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloDeprecatedSince.Version.v3_4_1
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import kotlin.jvm.JvmOverloads

enum class HttpMethod {
  Get, Post
}

interface HttpBody {
  val contentType: String

  /**
   * The number of bytes that will be written when calling [writeTo], or -1 if that count is unknown.
   */
  val contentLength: Long

  /**
   * This can be called several times
   */
  fun writeTo(bufferedSink: BufferedSink)
}

/**
 * a HTTP header
 */
data class HttpHeader(val name: String, val value: String)

/**
 * Get the value for header [name] or null if this header doesn't exist or is defined multiple times
 *
 * The header name matching is case-insensitive
 */
@ApolloExperimental
fun List<HttpHeader>.get(name: String): String? {
  return singleOrNull { it.name.lowercase() == name.lowercase() }?.value?.trim()
}

/**
 * a HTTP request to be sent
 */
class HttpRequest
private constructor(
    val method: HttpMethod,
    val url: String,
    val headers: List<HttpHeader>,
    val body: HttpBody?,
    val executionContext: ExecutionContext
) {

  @JvmOverloads
  fun newBuilder(method: HttpMethod = this.method, url: String = this.url) = Builder(
      method = method,
      url = url,
  ).apply {
    if (body != null) body(body)
    addHeaders(headers)
  }

  class Builder(
      private val method: HttpMethod,

      /**
       * The URL to send the request to.
       * Must conform to [RFC 3986](https://www.rfc-editor.org/rfc/rfc3986#section-2).
       */
      private val url: String,
  ) {
    private var body: HttpBody? = null
    private val headers = mutableListOf<HttpHeader>()
    private var executionContext = ExecutionContext.Empty

    fun body(body: HttpBody) = apply {
      this.body = body
    }

    fun addHeader(name: String, value: String) = apply {
      headers += HttpHeader(name, value)
    }

    fun addHeaders(headers: List<HttpHeader>) = apply {
      this.headers.addAll(headers)
    }

    fun addExecutionContext(executionContext: ExecutionContext) = apply {
      this.executionContext += executionContext
    }

    fun headers(headers: List<HttpHeader>) = apply {
      this.headers.clear()
      this.headers.addAll(headers)
    }

    fun build() = HttpRequest(
        method = method,
        url = url,
        headers = headers,
        body = body,
        executionContext = executionContext
    )
  }
}

/**
 * an HTTP Response.
 *
 * Specifying both [bodySource] and [bodyString] is invalid
 *
 * The [body] of a [HttpResponse] must always be closed if non-null
 */
class HttpResponse
private constructor(
    val statusCode: Int,
    val headers: List<HttpHeader>,
    /**
     * A streamable body.
     */
    private val bodySource: BufferedSource?,
    /**
     * An immutable body.
     * Prefer [bodySource] on so that the response can be streamed.
     */
    private val bodyString: ByteString?,
) {

  val body: BufferedSource?
    get() = bodySource ?: bodyString?.let { Buffer().write(it) }

  fun newBuilder() = Builder(
      statusCode = statusCode,
  ).apply {
    if (bodySource != null) body(bodySource)
    @Suppress("DEPRECATION")
    if (bodyString != null) body(bodyString)
    addHeaders(headers)
  }

  class Builder(
      val statusCode: Int,
  ) {
    private var bodySource: BufferedSource? = null
    private var bodyString: ByteString? = null
    private val headers = mutableListOf<HttpHeader>()
    private val hasBody: Boolean
      get() = bodySource != null || bodyString != null

    /**
     * A streamable body.
     */
    fun body(bodySource: BufferedSource) = apply {
      check(!hasBody) { "body() can only be called once" }
      this.bodySource = bodySource
    }

    /**
     * An immutable body.
     * Prefer [bodySource] so that the response can be streamed.
     */
    @Deprecated("Use body(BufferedSource) instead", ReplaceWith("Buffer().write(bodyString)", "okio.Buffer"))
    @ApolloDeprecatedSince(v3_4_1)
    fun body(bodyString: ByteString) = apply {
      check(!hasBody) { "body() can only be called once" }
      this.bodyString = bodyString
    }

    fun addHeader(name: String, value: String) = apply {
      headers += HttpHeader(name, value)
    }

    fun addHeaders(headers: List<HttpHeader>) = apply {
      this.headers.addAll(headers)
    }

    fun headers(headers: List<HttpHeader>) = apply {
      this.headers.clear()
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
