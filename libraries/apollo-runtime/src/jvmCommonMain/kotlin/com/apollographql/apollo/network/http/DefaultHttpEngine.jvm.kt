@file:JvmName("DefaultHttpEngine")

package com.apollographql.apollo.network.http

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.http.CacheUrlOverride
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
import okhttp3.HttpUrl.Companion.toHttpUrl
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

actual fun DefaultHttpEngine(timeoutMillis: Long): HttpEngine = DefaultHttpEngine {
  defaultOkHttpClientBuilder
      .connectTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
      .readTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
      .build()
}

fun DefaultHttpEngine(connectTimeoutMillis: Long, readTimeoutMillis: Long): HttpEngine = DefaultHttpEngine {
  defaultOkHttpClientBuilder
      .connectTimeout(connectTimeoutMillis, TimeUnit.MILLISECONDS)
      .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
      .build()
}

@Deprecated("Initializing an `OkHttpClient` from the main thread may be expensive. Use `DefaultHttpEngine(() -> Call.Factory)` instead")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
fun DefaultHttpEngine(httpCallFactory: Call.Factory): HttpEngine = DefaultHttpEngine { httpCallFactory }

@Deprecated("Initializing an `OkHttpClient` from the main thread may be expensive. Use `DefaultHttpEngine(() -> Call.Factory)` instead")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
fun DefaultHttpEngine(okHttpClient: OkHttpClient): HttpEngine = DefaultHttpEngine { okHttpClient }

fun DefaultHttpEngine(httpCallFactory: () -> Call.Factory): HttpEngine = OkHttpEngineImpl(lazyCallFactory = httpCallFactory)


private class OkHttpEngineImpl(
    private val lazyCallFactory: () -> Call.Factory,
) : HttpEngine {
  private val callFactory by lazy { lazyCallFactory() }

  override suspend fun execute(request: HttpRequest): HttpResponse {
    return callFactory.execute(request.toOkHttpRequest()).toApolloHttpResponse()
  }

  override fun close() {}

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
              val okHttpBody = object : RequestBody() {
                override fun contentType() = body.contentType.toMediaType()

                override fun contentLength() = body.contentLength

                // This prevents OkHttp from reading the body several times (e.g. when using its logging interceptor)
                // which could consume files when using Uploads
                override fun isOneShot() = body is UploadsHttpBody

                override fun writeTo(sink: BufferedSink) {
                  body.writeTo(sink)
                }
              }
              post(okHttpBody)
              val cacheUrlOverride = this@toOkHttpRequest.executionContext[CacheUrlOverride]?.url
              if (cacheUrlOverride != null) {
                cacheUrlOverride(cacheUrlOverride.toHttpUrl())
              }
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
          .body(body.source())
          .addHeaders(
              headers.let { headers ->
                0.until(headers.size).map { index ->
                  HttpHeader(headers.name(index), headers.value(index))
                }
              }
          )
          .addExecutionContext(IsFromHttpCache(cacheResponse != null))
          .build()
    }
  }
}

internal class IsFromHttpCache(val isFromHttpCache: Boolean) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<IsFromHttpCache>
    get() = Key

  companion object Key : ExecutionContext.Key<IsFromHttpCache>
}

/**
 * Returns true if the body of that response comes from the HTTP cache.
 *
 * In some cases (HTTP 304 Not Modified), a network request may still have happened to get the headers.
 */
val <D : Operation.Data> ApolloResponse<D>.isFromHttpCache: Boolean
  get() {
    return executionContext[IsFromHttpCache]?.isFromHttpCache == true
  }