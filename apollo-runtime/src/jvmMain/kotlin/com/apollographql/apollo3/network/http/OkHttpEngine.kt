package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.exception.ApolloNetworkException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class MultiplatformHttpEngine(
    private val httpCallFactory: Call.Factory,
) : HttpEngine {

  // an overload that takes an OkHttpClient for easier discovery
  constructor(okHttpClient: OkHttpClient) : this(okHttpClient as Call.Factory)

  actual constructor(
      connectTimeoutMillis: Long,
      readTimeoutMillis: Long,
  ) : this(
      httpCallFactory = OkHttpClient.Builder()
          .connectTimeout(connectTimeoutMillis, TimeUnit.MILLISECONDS)
          .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
          .build(),
  )

  override suspend fun execute(request: HttpRequest): HttpResponse = suspendCancellableCoroutine { continuation ->
    val httpRequest = Request.Builder()
        .url(request.url)
        .headers(
            Headers.Builder().apply {
              request.headers.forEach {
                this.add(it.name, it.value)
              }
            }.build()
        )
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

    var exception: IOException? = null
    val response = try {
      call.execute()
    } catch (e: IOException) {
      exception = e
      null
    }

    if (exception != null) {
      continuation.resumeWithException(
          ApolloNetworkException(
              message = "Failed to execute GraphQL http network request",
              platformCause = exception
          )
      )
      return@suspendCancellableCoroutine
    } else {
      val result = Result.success(
          HttpResponse.Builder(statusCode = response!!.code())
              .body(response.body()!!.source())
              .addHeaders(
                  response.headers().let { headers ->
                    0.until(headers.size()).map { index ->
                      HttpHeader(headers.name(index), headers.value(index))
                    }
                  }
              )
              .build()
      )
      continuation.resume(result.getOrThrow())
    }
  }

  override fun dispose() {
  }
}

fun HttpNetworkTransport(
    serverUrl: String,
    callFactory: Call.Factory,
): HttpNetworkTransport {
  return HttpNetworkTransport(
      serverUrl = serverUrl,
      engine = MultiplatformHttpEngine(callFactory)
  )
}

fun HttpNetworkTransport(
    serverUrl: String,
    okHttpClient: OkHttpClient,
): HttpNetworkTransport {
  return HttpNetworkTransport(
      serverUrl = serverUrl,
      engine = MultiplatformHttpEngine(okHttpClient)
  )
}
