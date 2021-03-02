package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.exception.ApolloParseException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

private const val MEDIA_TYPE = "application/json; charset=utf-8"

@ExperimentalCoroutinesApi
actual class DefaultHttpEngine(
    private val httpCallFactory: Call.Factory,
): HttpEngine {

  actual constructor(
      connectTimeoutMillis: Long,
      readTimeoutMillis: Long
  ) : this(
      httpCallFactory = OkHttpClient.Builder()
          .connectTimeout(connectTimeoutMillis, TimeUnit.MILLISECONDS)
          .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
          .build(),
  )

  override suspend fun <R> execute(request: HttpRequest, block: (HttpResponse) -> R): R {
    return suspendCancellableCoroutine { continuation ->
      val httpRequest = Request.Builder()
          .url(request.url)
          .headers(Headers.of(request.headers))
          .apply {
            if (request.method == HttpMethod.Get) {
              get()
            } else {
              check(request.body != null) {
                "HTTP POST requires a request body"
              }
              post(object: RequestBody() {
                override fun contentType() = MediaType.parse(request.body.contentType)

                override fun contentLength() = request.body.contentLength

                override fun writeTo(sink: BufferedSink) {
                  request.body.writeTo(sink)
                }
              })
            }
          }
          .build()

      httpCallFactory.newCall(httpRequest)
          .also { call ->
            continuation.invokeOnCancellation {
              call.cancel()
            }
          }
          .enqueue(
              object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                  if (continuation.isCancelled) return
                  continuation.resumeWithException(
                      ApolloNetworkException(
                          message = "Failed to execute GraphQL http network request",
                          cause = e
                      )
                  )
                }

                override fun onResponse(call: Call, response: okhttp3.Response) {
                  if (continuation.isCancelled) return
                  runCatching {
                    val httpResponse = HttpResponse(
                        statusCode = response.code(),
                        headers = response.headers().toMap(),
                        body = response.body()!!.source()
                    )
                    block(httpResponse)
                  }
                      .onSuccess { continuation.resume(it) }
                      .onFailure { e ->
                        if (e is ApolloException) {
                          continuation.resumeWithException(e)
                        } else {
                          // Most likely a Json error, we should make them ApolloException
                          @Suppress("ThrowableNotThrown")
                          continuation.resumeWithException(
                              ApolloParseException(
                                  message = "Failed to parse GraphQL http network response",
                                  cause = e
                              )
                          )
                        }
                      }
                }
              }
          )
    }
  }

  private fun Headers.toMap(): Map<String, String> {
    return names().map {
      it to get(it)!!
    }.toMap()
  }
}
