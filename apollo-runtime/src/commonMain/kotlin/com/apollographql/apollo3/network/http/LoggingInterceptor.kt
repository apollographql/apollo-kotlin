package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.http.ByteStringHttpBody
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import okio.Buffer
import kotlin.jvm.JvmOverloads

/**
 * An interceptor that logs requests and responses.
 *
 * @param logRequestBody whether to log the request body. Caution: when uploading files, setting this to `true` will cause the files to
 * be fully loaded into memory, which may cause OutOfMemoryErrors.
 */
class LoggingInterceptor(
    private val logRequestBody: Boolean,
    private val log: (String) -> Unit = { println(it) },
) : HttpInterceptor {
  @JvmOverloads
  constructor(log: (String) -> Unit = { println(it) }) : this(true, log)

  override suspend fun intercept(
      request: HttpRequest,
      chain: HttpInterceptorChain,
  ): HttpResponse {
    log("${request.method.name} ${request.url}")

    request.headers.forEach {
      log("${it.name}: ${it.value}")
    }
    log("[end of headers]")

    val newRequest =
        if (!logRequestBody) {
          log("[request body omitted]")
          request
        } else {
          val buffer = Buffer()
          request.body?.writeTo(buffer)
          val bodyByteString = buffer.readByteString()
          log(bodyByteString.utf8())
          request.newBuilder()
              .apply {
                request.body?.let { originalBody ->
                  body(ByteStringHttpBody(contentType = originalBody.contentType, bodyByteString))
                }
              }.build()
        }

    log("")

    val httpResponse = chain.proceed(newRequest)
    log("HTTP: ${httpResponse.statusCode}")

    httpResponse.headers.forEach {
      log("${it.name}: ${it.value}")
    }
    log("[end of headers]")

    val body = httpResponse.body?.readByteString()
    if (body != null) {
      log(body.utf8())
    }

    return HttpResponse.Builder(statusCode = httpResponse.statusCode)
        .also { if (body != null) it.body(body) }
        .addHeaders(httpResponse.headers)
        .build()
  }
}
