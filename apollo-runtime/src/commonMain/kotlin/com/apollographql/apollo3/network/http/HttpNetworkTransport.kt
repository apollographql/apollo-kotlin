package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpRequestComposer
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.api.parseJsonResponse
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloParseException
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.internal.NonMainWorker
import com.apollographql.apollo3.network.NetworkTransport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class HttpNetworkTransport(
    private val httpRequestComposer: HttpRequestComposer,
    val engine: HttpEngine = DefaultHttpEngine(),
    val interceptors: List<HttpInterceptor> = emptyList(),
) : NetworkTransport {
  private val worker = NonMainWorker()

  /**
   *
   * @param serverUrl
   * @param connectTimeoutMillis The timeout interval to use when connecting
   *
   * - on iOS, it is used to set [NSMutableURLRequest.timeoutInterval]
   * - on Android, it is used to set [OkHttpClient.connectTimeout]
   *
   * @param readTimeoutMillis The timeout interval to use when waiting for additional data.
   *
   * - on iOS, it is used to set [NSURLSessionConfiguration.timeoutIntervalForRequest]
   * - on Android, it is used to set  [OkHttpClient.readTimeout]
   */
  constructor(
      serverUrl: String,
      connectTimeoutMillis: Long = 60_000,
      readTimeoutMillis: Long = 60_000,
      interceptors: List<HttpInterceptor> = emptyList(),
  ) : this(
      DefaultHttpRequestComposer(serverUrl),
      DefaultHttpEngine(connectTimeoutMillis, readTimeoutMillis),
      interceptors
  )

  /**
   *
   * @param serverUrl
   * @param connectTimeoutMillis The timeout interval to use when connecting
   *
   * - on iOS, it is used to set [NSMutableURLRequest.timeoutInterval]
   * - on Android, it is used to set [OkHttpClient.connectTimeout]
   *
   * @param readTimeoutMillis The timeout interval to use when waiting for additional data.
   *
   * - on iOS, it is used to set [NSURLSessionConfiguration.timeoutIntervalForRequest]
   * - on Android, it is used to set  [OkHttpClient.readTimeout]
   */
  constructor(
      serverUrl: String,
      engine: HttpEngine,
      interceptors: List<HttpInterceptor> = emptyList(),
  ) : this(
      DefaultHttpRequestComposer(serverUrl),
      engine,
      interceptors
  )

  private val engineInterceptor = EngineInterceptor()

  override fun <D : Operation.Data> execute(
      request: ApolloRequest<D>,
  ): Flow<ApolloResponse<D>> {
    val customScalarAdapters = request.executionContext[CustomScalarAdapters]!!
    val httpRequest = httpRequestComposer.compose(request)

    return execute(request, httpRequest, customScalarAdapters)
  }

  fun <D : Operation.Data> execute(
      request: ApolloRequest<D>,
      httpRequest: HttpRequest,
      customScalarAdapters: CustomScalarAdapters,
  ): Flow<ApolloResponse<D>> {
    return flow {
      val httpResponse = RealInterceptorChain(
          interceptors = interceptors + engineInterceptor,
          index = 0
      ).proceed(httpRequest)

      if (httpResponse.statusCode !in 200..299) {
        throw ApolloHttpException(
            statusCode = httpResponse.statusCode,
            headers = httpResponse.headers,
            message = "Http request failed with status code `${httpResponse.statusCode} (${httpResponse.body?.readUtf8()})`"
        )
      }

      // do not capture request
      val operation = request.operation
      val response = worker.doWork {
        try {
          operation.parseJsonResponse(
              source = httpResponse.body!!,
              customScalarAdapters = customScalarAdapters
          )
        } catch (e: Exception) {
          throw wrapThrowableIfNeeded(e)
        }
      }

      emit(
          response.copy(
              requestUuid = request.requestUuid,
              executionContext = request.executionContext + HttpInfo(
                  statusCode = httpResponse.statusCode,
                  headers = httpResponse.headers
              )
          )
      )
    }
  }

  inner class EngineInterceptor : HttpInterceptor {
    override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
      return engine.execute(request)
    }
  }

  override fun dispose() {
    engine.dispose()
  }

  /**
   * Creates a copy of the [HttpNetworkTransport]
   *
   * The copy will own the [engine]. It is an error to call [dispose] after [copy] on the original instance
   */
  fun copy(
      httpRequestComposer: HttpRequestComposer = this.httpRequestComposer,
      engine: HttpEngine = this.engine,
      interceptors: List<HttpInterceptor> = this.interceptors
  ): HttpNetworkTransport {
    return HttpNetworkTransport(
        httpRequestComposer = httpRequestComposer,
        engine = engine,
        interceptors = interceptors
    )
  }

  companion object {
    private fun wrapThrowableIfNeeded(throwable: Throwable): ApolloException {
      return if (throwable is ApolloException) {
        throwable
      } else {
        // This happens for null pointer exceptions on missing fields
        ApolloParseException(
            message = "Failed to parse GraphQL http network response",
            cause = throwable
        )
      }
    }
  }
}

/**
 * Adds a new [HeadersInterceptor] that will add [headers] to each [HttpRequest]
 */
fun HttpNetworkTransport.withDefaultHeaders(headers: List<HttpHeader>) = copy (interceptors = this.interceptors + HeadersInterceptor(headers))