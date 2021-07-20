package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.js.Js
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.http.HttpHeaders
import io.ktor.util.toMap
import okio.Buffer
import okio.ByteString.Companion.toByteString

actual class DefaultHttpEngine actual constructor(connectTimeoutMillis: Long, readTimeoutMillis: Long) : HttpEngine {
  private val client = HttpClient(Js)

  override suspend fun execute(request: HttpRequest): HttpResponse {
    val response = client.request<io.ktor.client.statement.HttpResponse>(request.url) {
      method = when (request.method) {
        HttpMethod.Get -> io.ktor.http.HttpMethod.Get
        HttpMethod.Post -> io.ktor.http.HttpMethod.Post
      }
      request.headers.forEach {
        header(it.key, it.value)
      }
      request.body?.let {
        header(HttpHeaders.ContentType, it.contentType)
        val buffer = Buffer()
        it.writeTo(buffer)
        body = buffer.readUtf8()
      }
    }
    val responseByteArray: ByteArray = response.receive()
    return HttpResponse(
        response.status.value,
        response.headers.toMap().mapValues { it.value.first() },
        Buffer().write(responseByteArray),
        responseByteArray.toByteString()
    )
  }

  override fun dispose() {
    client.close()
  }
}