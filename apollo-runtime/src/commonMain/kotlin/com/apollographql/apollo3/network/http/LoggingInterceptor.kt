package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.http.ByteStringHttpBody
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.network.http.LoggingInterceptor.Level
import okio.Buffer
import kotlin.jvm.JvmOverloads

/**
 * An interceptor that logs requests and responses.
 *
 * @param level the level of logging. Caution: when uploading files, setting this to [Level.BODY] will cause the files to
 * be fully loaded into memory, which may cause OutOfMemoryErrors.
 */
class LoggingInterceptor(
    private val level: Level,
    private val log: (String) -> Unit = { println(it) },
) : HttpInterceptor {
  @JvmOverloads
  constructor(log: (String) -> Unit = { println(it) }) : this(level = Level.BODY, log = log)

  enum class Level {
    NONE,
    BASIC,
    HEADERS,
    BODY,
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
      val bodyByteString = responseBody.readByteString()
      log(bodyByteString.utf8())
      HttpResponse.Builder(statusCode = httpResponse.statusCode)
          .body(bodyByteString)
          .addHeaders(httpResponse.headers)
          .build()
    }
  }
}
