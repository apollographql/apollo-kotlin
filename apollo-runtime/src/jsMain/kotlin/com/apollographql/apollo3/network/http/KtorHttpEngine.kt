package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.exception.ApolloNetworkException
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.js.Js
import io.ktor.client.features.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.http.HttpHeaders
import io.ktor.util.flattenEntries
import okio.Buffer

actual class DefaultHttpEngine constructor(private val connectTimeoutMillis: Long, private val readTimeoutMillis: Long) : HttpEngine {
  var disposed = false

  actual constructor(timeoutMillis: Long): this(timeoutMillis, timeoutMillis)

  private val client = HttpClient(Js) {
    expectSuccess = false
    install(HttpTimeout) {
      this.connectTimeoutMillis = this@DefaultHttpEngine.connectTimeoutMillis
      this.socketTimeoutMillis = this@DefaultHttpEngine.readTimeoutMillis
    }
  }

  override suspend fun execute(request: HttpRequest): HttpResponse {
    try {
      val response = client.request<io.ktor.client.statement.HttpResponse>(request.url) {
        method = when (request.method) {
          HttpMethod.Get -> io.ktor.http.HttpMethod.Get
          HttpMethod.Post -> io.ktor.http.HttpMethod.Post
        }
        request.headers.forEach {
          header(it.name, it.value)
        }
        request.body?.let {
          header(HttpHeaders.ContentType, it.contentType)
          val buffer = Buffer()
          it.writeTo(buffer)
          body = buffer.readUtf8()
        }
      }
      val responseByteArray: ByteArray = response.receive()
      val responseBufferedSource = Buffer().write(responseByteArray)
      return HttpResponse.Builder(statusCode = response.status.value)
          .body(responseBufferedSource)
          .addHeaders(response.headers.flattenEntries().map { HttpHeader(it.first, it.second) })
          .build()

    } catch (t: Throwable) {
      throw ApolloNetworkException(t.message, t)
    }
  }

  override fun dispose() {
    if (!disposed) {
      client.close()
      disposed = true
    }
  }
}
