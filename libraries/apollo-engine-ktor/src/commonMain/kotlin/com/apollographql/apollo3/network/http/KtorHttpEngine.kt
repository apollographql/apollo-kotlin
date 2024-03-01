package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.exception.ApolloNetworkException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.util.flattenEntries
import okio.Buffer
import kotlin.coroutines.cancellation.CancellationException

@ApolloExperimental
class KtorHttpEngine(
    private val client: HttpClient,
): HttpEngine {

  private var disposed = false

  /**
   * @param timeoutMillis: The timeout in milliseconds used both for the connection and socket read.
   */
  constructor(timeoutMillis: Long = 60_000) : this(timeoutMillis, timeoutMillis)

  /**
   * @param connectTimeoutMillis The connection timeout in milliseconds. The connection timeout is the time period in which a client should establish a connection with a server.
   * @param readTimeoutMillis The socket read timeout in milliseconds. On JVM and Apple this maps to [HttpTimeout.HttpTimeoutCapabilityConfiguration.socketTimeoutMillis], on JS
   * this maps to [HttpTimeout.HttpTimeoutCapabilityConfiguration.requestTimeoutMillis]
   */
  constructor(connectTimeoutMillis: Long, readTimeoutMillis: Long) : this(
      HttpClient {
        expectSuccess = false
        install(HttpTimeout) {
          this.connectTimeoutMillis = connectTimeoutMillis
          setReadTimeout(readTimeoutMillis)
        }
      }
  )

  override suspend fun execute(request: HttpRequest): HttpResponse {
    try {
      val response = client.request(request.url) {
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
          setBody(buffer.readUtf8())
        }
      }
      val responseByteArray: ByteArray = response.body()
      val responseBufferedSource = Buffer().write(responseByteArray)
      return HttpResponse.Builder(statusCode = response.status.value)
          .body(responseBufferedSource)
          .addHeaders(response.headers.flattenEntries().map { HttpHeader(it.first, it.second) })
          .build()
    } catch (e: CancellationException) {
      // Cancellation Exception is passthrough
      throw e
    } catch (t: Throwable) {
      throw ApolloNetworkException(t.message, t)
    }
  }

  override fun close() {
    if (!disposed) {
      client.close()
      disposed = true
    }
  }
}

@ApolloInternal
expect fun HttpTimeout.HttpTimeoutCapabilityConfiguration.setReadTimeout(readTimeoutMillis: Long)
