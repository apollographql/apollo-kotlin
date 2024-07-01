package com.apollographql.apollo.network.http

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloDeprecatedSince.Version.v4_0_0
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.http.HttpBody
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.exception.ApolloNetworkException
import okio.Closeable

/**
 * A wrapper around platform specific engines
 */
interface HttpEngine : Closeable {

  /**
   * Executes the given HttpRequest
   *
   * HTTP errors should not throw but instead return a [HttpResponse] indicating the status code
   *
   * @throws [ApolloNetworkException] if a network error happens
   */
  suspend fun execute(request: HttpRequest): HttpResponse

  @ApolloDeprecatedSince(v4_0_0)
  @Deprecated("Use close", ReplaceWith("close()"), level = DeprecationLevel.ERROR)
  fun dispose() {
  }

  /**
   * Disposes any resources used by the [HttpEngine]
   *
   * Use this to dispose a connection pool for an example. Must be idemptotent
   */
  @Suppress("DEPRECATION_ERROR")
  override fun close() = dispose()
}

/**
 * @param timeoutMillis The timeout interval to use when connecting or waiting for additional data.
 *
 * - on iOS (NSURLRequest), it is used to set `NSMutableURLRequest.setTimeoutInterval`
 * - on Android (OkHttp), it is used to set both `OkHttpClient.connectTimeout` and `OkHttpClient.readTimeout`
 * - on Js (Ktor), it is used to set both `HttpTimeoutCapabilityConfiguration.connectTimeoutMillis` and `HttpTimeoutCapabilityConfiguration.requestTimeoutMillis`
 */
expect fun DefaultHttpEngine(timeoutMillis: Long = 60_000): HttpEngine

fun HttpEngine.get(url: String) = HttpCall(this, HttpMethod.Get, url)
fun HttpEngine.post(url: String) = HttpCall(this, HttpMethod.Post, url)

class HttpCall(private val engine: HttpEngine, method: HttpMethod, url: String) {
  private val requestBuilder = HttpRequest.Builder(method, url)

  fun body(body: HttpBody) = apply {
    requestBuilder.body(body)
  }

  fun addHeader(name: String, value: String) = apply {
    requestBuilder.addHeader(name, value)
  }

  fun addHeaders(headers: List<HttpHeader>) = apply {
    requestBuilder.addHeaders(headers)
  }

  fun addExecutionContext(executionContext: ExecutionContext) = apply {
    requestBuilder.addExecutionContext(executionContext)
  }

  fun headers(headers: List<HttpHeader>) = apply {
    requestBuilder.headers(headers)
  }

  suspend fun execute(): HttpResponse = engine.execute(requestBuilder.build())
}
