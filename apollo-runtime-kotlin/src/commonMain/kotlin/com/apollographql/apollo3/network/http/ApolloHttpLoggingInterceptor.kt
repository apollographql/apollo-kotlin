package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import okio.Buffer

class ApolloHttpLoggingInterceptor : HttpRequestInterceptor {
  override suspend fun <R> intercept(request: HttpRequest, block: (HttpResponse) -> R, chain: HttpInterceptorChain): R {
    println("${request.method.name} ${request.url}")

    request.headers.forEach {
      println("${it.key}: ${it.value}")
    }
    println("[end of headers]")

    val buffer = Buffer()
    request.body?.writeTo(buffer)
    println(buffer.readUtf8())

    println("")

    return chain.proceed(request) { httpResponse ->
      println("HTTP: ${httpResponse.statusCode}")

      httpResponse.headers.forEach {
        println("${it.key}: ${it.value}")
      }
      println("[end of headers]")

      val body = httpResponse.body?.readByteString()
      if (body != null) {
        println(body.utf8())
      }

      block(
          HttpResponse(
              statusCode = httpResponse.statusCode,
              headers = httpResponse.headers,
              body = body?.let { Buffer().write(it) }
          )
      )
    }
  }
}