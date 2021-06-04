package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.api.exception.ApolloException
import com.apollographql.apollo3.api.exception.ApolloParseException

/**
 * A wrapper around platform specific engines
 */
interface HttpEngine {

  /**
   * @param block a function that will transform the response. Implementations can decide to run this in the IO thread
   * to keep the main thread free
   */
  suspend fun <R> execute(request: HttpRequest, block: (HttpResponse) -> R): R
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
): HttpEngine

fun wrapThrowableIfNeeded(throwable: Throwable): ApolloException {
  return if (throwable is ApolloException) {
    throwable
  } else {
    // Most likely a Json error, we should make them ApolloException
    ApolloParseException(
        message = "Failed to parse GraphQL http network response",
        cause = throwable
    )
  }
}