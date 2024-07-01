@file:JvmName("DefaultHttpEngine")

package com.apollographql.apollo.network.http

import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.api.http.UploadsHttpBody
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.network.defaultOkHttpClientBuilder
import com.apollographql.apollo.network.toOkHttpHeaders
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual fun DefaultHttpEngine(timeoutMillis: Long): HttpEngine = JvmHttpEngine(timeoutMillis)

fun DefaultHttpEngine(httpCallFactory: Call.Factory): HttpEngine = JvmHttpEngine(httpCallFactory)

fun DefaultHttpEngine(httpCallFactory: () -> Call.Factory): HttpEngine = JvmHttpEngine(httpCallFactory)

fun DefaultHttpEngine(okHttpClient: OkHttpClient): HttpEngine = JvmHttpEngine(okHttpClient)

fun DefaultHttpEngine(connectTimeoutMillis: Long, readTimeoutMillis: Long): HttpEngine = JvmHttpEngine(connectTimeoutMillis, readTimeoutMillis)

private class JvmHttpEngine(
    private val httpCallFactory: () -> Call.Factory,
) : HttpEngine {
  private val callFactory by lazy { httpCallFactory() }

  constructor(httpCallFactory: Call.Factory) : this({ httpCallFactory })

  constructor(timeoutMillis: Long) : this(timeoutMillis, timeoutMillis)

  constructor(connectTimeoutMillis: Long, readTimeoutMillis: Long) : this(
      defaultOkHttpClientBuilder
          .connectTimeout(connectTimeoutMillis, TimeUnit.MILLISECONDS)
          .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
          .build()
  )

  override suspend fun execute(request: HttpRequest): HttpResponse {
    return callFactory.execute(request.toOkHttpRequest()).toApolloHttpResponse()
  }

  override fun close() {
  }

  companion object {
    fun HttpRequest.toOkHttpRequest(): Request {
      return Request.Builder()
          .url(url)
          .headers(headers.toOkHttpHeaders())
          .apply {
            if (method == HttpMethod.Get) {
              get()
            } else {
              val body = body
              check(body != null) {
                "HTTP POST requires a request body"
              }
              post(object : RequestBody() {
                override fun contentType() = body.contentType.toMediaType()

                override fun contentLength() = body.contentLength

                // This prevents OkHttp from reading the body several times (e.g. when using its logging interceptor)
                // which could consume files when using Uploads
                override fun isOneShot() = body is UploadsHttpBody

                override fun writeTo(sink: BufferedSink) {
                  body.writeTo(sink)
                }
              })
            }
          }
          .build()
    }

    suspend fun Call.Factory.execute(request: Request): Response = suspendCancellableCoroutine { continuation ->
      val call = newCall(request)
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
        continuation.resume(response!!)
      }
    }

    fun Response.toApolloHttpResponse(): HttpResponse {
      return HttpResponse.Builder(statusCode = code)
          .body(body!!.source())
          .addHeaders(
              headers.let { headers ->
                0.until(headers.size).map { index ->
                  HttpHeader(headers.name(index), headers.value(index))
                }
              }
          )
          .build()
    }
  }
}


