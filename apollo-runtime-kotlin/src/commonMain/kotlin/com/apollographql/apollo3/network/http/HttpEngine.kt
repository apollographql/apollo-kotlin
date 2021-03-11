package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloParseException
import okio.BufferedSink
import okio.BufferedSource

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

interface HttpBody {
  val contentType: String
  val contentLength: Long
  fun writeTo(bufferedSink: BufferedSink)
}

class HttpRequest(
    val url: String,
    val headers: Map<String, String>,
    val method: HttpMethod,
    val body: HttpBody?
)

class HttpResponse(
    val statusCode: Int,
    val headers: Map<String, String>,
    /**
     * The actual body
     * It must always be closed if not null
     */
    val body: BufferedSource?,
)

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