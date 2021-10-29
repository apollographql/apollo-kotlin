package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import okio.Buffer

class LoggingInterceptor(private val log: (String) -> Unit = { println(it) }) : HttpInterceptor {
  override suspend fun intercept(
      request: HttpRequest,
      chain: HttpInterceptorChain,
  ): HttpResponse {
    log("${request.method.name} ${request.url}")

    request.headers.forEach {
      log("${it.name}: ${it.value}")
    }
    log("[end of headers]")

    val buffer = Buffer()
    request.body?.writeTo(buffer)
    log(buffer.readUtf8())

    log("")

    val httpResponse = chain.proceed(request)
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
