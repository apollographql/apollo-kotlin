package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.exception.ApolloNetworkException
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class DefaultHttpEngine(
    private val httpCallFactory: Call.Factory,
) : HttpEngine {

  actual constructor(
      connectTimeoutMillis: Long,
      readTimeoutMillis: Long,
  ) : this(
      httpCallFactory = OkHttpClient.Builder()
          .connectTimeout(connectTimeoutMillis, TimeUnit.MILLISECONDS)
          .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
          .build(),
  )

  override suspend fun <R> execute(request: HttpRequest, block: (HttpResponse) -> R): R = suspendCancellableCoroutine { continuation ->
    val httpRequest = Request.Builder()
        .url(request.url)
        .headers(Headers.of(request.headers))
        .apply {
          if (request.method == HttpMethod.Get) {
            get()
          } else {
            val body = request.body
            check(body != null) {
              "HTTP POST requires a request body"
            }
            post(object : RequestBody() {
              override fun contentType() = MediaType.parse(body.contentType)

              override fun contentLength() = body.contentLength

              override fun writeTo(sink: BufferedSink) {
                body.writeTo(sink)
              }
            })
          }
        }
        .build()

    val call = httpCallFactory.newCall(httpRequest)
    continuation.invokeOnCancellation {
      call.cancel()
    }

    val networkResult = kotlin.runCatching {
      call.execute()
    }

    if (networkResult.isFailure) {
      continuation.resumeWithException(
          ApolloNetworkException(
              message = "Failed to execute GraphQL http network request",
              cause = networkResult.exceptionOrNull()!!
          )
      )
      return@suspendCancellableCoroutine
    }

    val response = networkResult.getOrThrow()

    val parseResult = kotlin.runCatching {
      val httpResponse = HttpResponse(
          statusCode = response.code(),
          headers = response.headers().toMap(),
          body = response.body()!!.source()
      )
      block(httpResponse)
    }


    if (parseResult.isFailure) {
      continuation.resumeWithException(wrapThrowableIfNeeded(parseResult.exceptionOrNull()!!))
      return@suspendCancellableCoroutine
    }

    continuation.resume(parseResult.getOrThrow())
  }

  private fun Headers.toMap(): Map<String, String> {
    return names().map {
      it to get(it)!!
    }.toMap()
  }
}
