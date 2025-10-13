@file:JvmName("DefaultHttpEngine")

package com.apollographql.apollo.network.http

import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.api.http.UploadsHttpBody
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.apollo.network.defaultOkHttpClientBuilder
import com.apollographql.apollo.network.toOkHttpHeaders
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.ByteString.Companion.encode
import okio.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual fun DefaultHttpEngine(timeoutMillis: Long): HttpEngine = JvmHttpEngine(timeoutMillis)

fun DefaultHttpEngine(httpCallFactory: Call.Factory): HttpEngine = JvmHttpEngine(httpCallFactory)

fun DefaultHttpEngine(httpCallFactory: () -> Call.Factory): HttpEngine = JvmHttpEngine(httpCallFactory)

fun DefaultHttpEngine(okHttpClient: OkHttpClient): HttpEngine = JvmHttpEngine(okHttpClient)

fun DefaultHttpEngine(connectTimeoutMillis: Long, readTimeoutMillis: Long): HttpEngine =
  JvmHttpEngine(connectTimeoutMillis, readTimeoutMillis)

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

            val cacheUrlOverride = executionContext[CacheUrlOverride]?.url
            if (cacheUrlOverride != null) {
              this.cacheUrlOverride(cacheUrlOverride.toHttpUrl())
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

internal class CacheUrlOverride(val url: String) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<CacheUrlOverride>
    get() = Key

  companion object Key : ExecutionContext.Key<CacheUrlOverride>
}

/**
 * An interceptor that configures the cache url override to allow caching HTTP POST requests.
 *
 * @param baseUrl the baseUrl for the cache url override. If it does not end with '/',
 * a '/' is added.
 * For a given operation, the cache url override is `${baseUrlWithTrailingSlash}${operation.id()}`
 */
class CacheUrlOverrideInterceptor(baseUrl: String) : ApolloInterceptor {
  private var base: String

  init {
    this.base = buildString {
      append(baseUrl)
      if (!baseUrl.endsWith('/')) {
        append('/')
      }
    }
  }
  override fun <D : Operation.Data> intercept(
      request: ApolloRequest<D>,
      chain: ApolloInterceptorChain,
  ): Flow<ApolloResponse<D>> {
    val newRequest = if(request.operation is Query<*>) {
      request.newBuilder()
          .addExecutionContext(CacheUrlOverride(base + request.operation.id()))
          .build()
    } else {
      // do not cache mutations/subscriptions
      request
    }
    return chain.proceed(newRequest)
  }
}

/**
 * Returns true if the body of that response comes from the HTTP cache.
 *
 * In some cases (HTTP 304 Not Modified), a network request may still have happened to get the headers.
 */
val <D: Operation.Data> ApolloResponse<D>.isFromHttpCache: Boolean
  get() {
    return executionContext[IsFromHttpCache]?.isFromHttpCache == true
  }