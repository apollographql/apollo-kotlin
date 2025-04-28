package com.apollographql.apollo.network.http

import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.util.StringValuesImpl
import io.ktor.util.flattenEntries
import io.ktor.utils.io.asSource
import kotlinx.io.okio.asOkioSource
import okio.buffer

actual fun DefaultHttpEngine(timeoutMillis: Long): HttpEngine = LinuxHttpEngine()

private class LinuxHttpEngine() : HttpEngine {

  private val client = HttpClient(Curl)

  override suspend fun execute(request: HttpRequest): HttpResponse {
    val response = client.request(urlString = request.url) {
      method = when (request.method) {
        HttpMethod.Get -> io.ktor.http.HttpMethod.Get
        HttpMethod.Post -> io.ktor.http.HttpMethod.Post
      }
      setBody(request.body)
      headers.appendAll(
          StringValuesImpl(
              values = request.headers.associate { header ->
                header.name to listOf(header.value)
              }
          )
      )
    }

    return HttpResponse.Builder(response.status.value)
        .headers(response.headers.flattenEntries().map { (name, value) ->
          HttpHeader(name, value)
        })
        .body(response.bodyAsChannel().asSource().asOkioSource().buffer())
        .build()
  }

  override fun close() {
    client.close()
  }
}
