package com.apollographql.apollo.network.http

import com.apollographql.apollo.api.http.ByteStringHttpBody
import com.apollographql.apollo.api.http.HttpBody
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.api.http.valueOf
import com.apollographql.apollo.network.http.LoggingInterceptor.Level
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.Sink
import okio.Source
import okio.Timeout
import okio.buffer
import okio.use
import kotlin.jvm.JvmOverloads

/**
 * An interceptor that logs requests and responses.
 *
 * @param level the level of logging. Caution: when uploading files, setting this to [Level.BODY] will cause the files to
 * be fully loaded into memory, which may cause OutOfMemoryErrors.
 *
 * @param log a callback that gets called with a line of log. Contains headers and/or body. No attempt is made at detecting
 * binary bodies. Do use [Level.BODY] with binary content
 */
class LoggingInterceptor(
    private val level: Level,
    private val log: (String) -> Unit = { println(it) },
) : HttpInterceptor {
  @JvmOverloads
  constructor(log: (String) -> Unit = { println(it) }) : this(level = Level.BODY, log = log)

  enum class Level {
    /** No logs. */
    NONE,

    /**
     * Logs HTTP method, request target, and response code.
     *
     * Example:
     * ```
     * Post /graphql
     *
     * HTTP: 200
     * ```
     */
    BASIC,
    
    /**
     * Logs HTTP method, request target, response code, and headers.
     *
     * Example:
     * ```
     * Post /graphql
     * X-APOLLO-OPERATION-ID: 9311
     * Accept: multipart/mixed; deferSpec=20220824, application/json
     * [end of headers]
     *
     * HTTP: 200
     * Content-Type: application/json; charset=utf-8
     * Content-Length: 2716
     * [end of headers]
     * ```
     */
    HEADERS,
     
    /**
     * Logs HTTP method, request target, response code, headers, and bodies.
     *
     * Example:
     * ```
     * Post /graphql
     * X-APOLLO-OPERATION-ID: 9311
     * [end of headers]
     * {"operationName":"LaunchList","variables":{"cursor":"1584533760"},"query":"query LaunchList($cursor: String)}
     *
     * HTTP: 200
     * Content-Type: application/json; charset=utf-8
     * Content-Length: 2716
     * [end of headers]
     * {"data":{"launches":{"cursor":"1544033760","hasMore":true,"launches":[...]}}}
     * ```
     */
    BODY,
  }

  private fun BufferedSource.intercept(): Source {
    return object: Source {
      private val buffer = Buffer()

      override fun close() {
        this@intercept.close()
      }

      override fun read(sink: Buffer, byteCount: Long): Long {
        val tmp = Buffer()
        val read = this@intercept.read(tmp, byteCount)
        buffer.writeAll(tmp.peek())
        while (true) {
          buffer.readUtf8Line()?.let { log(it) } ?: break
        }
        sink.writeAll(tmp)
        return read
      }

      override fun timeout(): Timeout {
        return Timeout.NONE
      }
    }
  }

  override suspend fun intercept(
      request: HttpRequest,
      chain: HttpInterceptorChain,
  ): HttpResponse {
    if (level == Level.NONE) {
      return chain.proceed(request)
    }
    val logHeaders = level == Level.HEADERS || level == Level.BODY
    val logBody = level == Level.BODY

    log("${request.method.name} ${request.url}")

    if (logHeaders) {
      request.headers.forEach {
        log("${it.name}: ${it.value}")
      }
      log("[end of headers]")
    }

    val requestBody = request.body
    val newRequest = if (!logBody || requestBody == null) {
      request
    } else {
      val buffer = Buffer()
      requestBody.writeTo(buffer)
      val bodyByteString = buffer.readByteString()
      log(bodyByteString.utf8())
      request.newBuilder()
          .body(ByteStringHttpBody(contentType = requestBody.contentType, bodyByteString))
          .build()
    }

    log("")
    val httpResponse = chain.proceed(newRequest)

    log("HTTP: ${httpResponse.statusCode}")

    if (logHeaders) {
      httpResponse.headers.forEach {
        log("${it.name}: ${it.value}")
      }
      log("[end of headers]")
    }

    val responseBody = httpResponse.body
    return if (!logBody || responseBody == null) {
      httpResponse
    } else {
      HttpResponse.Builder(statusCode = httpResponse.statusCode)
          .body(responseBody.intercept().buffer())
          .addHeaders(httpResponse.headers)
          .build()
    }
  }
}
