package com.apollographql.apollo3.network.http

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
   * Use this to dispose a connection pool for an example
   */
  fun dispose()
}

expect class DefaultHttpEngine(
    /**
     * The timeout interval to use when connecting
     *
     * - on iOS, it is used to set [NSMutableURLRequest.timeoutInterval]
     * - on Android, it is used to set [OkHttpClient.connectTimeout]
     */
    connectTimeoutMillis: Long = 60_000,
    /**
     * The timeout interval to use when waiting for additional data.
     *
     * - on iOS, it is used to set [NSURLSessionConfiguration.timeoutIntervalForRequest]
     * - on Android, it is used to set  [OkHttpClient.readTimeout]
     */
    readTimeoutMillis: Long = 60_000,
) : HttpEngine

