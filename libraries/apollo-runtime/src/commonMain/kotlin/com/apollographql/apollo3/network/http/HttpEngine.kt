package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.http.HttpBody
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.exception.ApolloNetworkException

/**
 * A wrapper around platform specific engines
 */
interface HttpEngine {

  /**
   * Executes the given HttpRequest
   *
   * throws [ApolloNetworkException] if a network error happens
   * HTTP errors should not throw but instead return a [HttpResponse] indicating the status code
   */
  suspend fun execute(request: HttpRequest): HttpResponse

  /**
   * Disposes any resources used by the [HttpEngine]
   *
   * Use this to dispose a connection pool for an example. Must be idemptotent
   */
  fun dispose()
}

/**
 * @param timeoutMillis: The timeout interval to use when connecting or waiting for additional data.
 *
 * - on iOS, it is used to set [NSMutableURLRequest.timeoutIntervalForRequest]
 * - on Android, it is used to set both [OkHttpClient.connectTimeout] and [OkHttpClient.readTimeout]
 * - on Js, it is used to set both connectTimeoutMillis, and socketTimeoutMillis
 */
expect class DefaultHttpEngine(timeoutMillis: Long = 60_000): HttpEngine

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

  fun headers(headers: List<HttpHeader>) = apply {
    requestBuilder.headers(headers)
  }

  suspend fun execute(): HttpResponse = engine.execute(requestBuilder.build())
}