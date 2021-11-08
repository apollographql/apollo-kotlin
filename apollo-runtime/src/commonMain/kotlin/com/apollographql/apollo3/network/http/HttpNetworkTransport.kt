package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpRequestComposer
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.api.parseJsonResponse
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloParseException
import com.apollographql.apollo3.internal.NonMainWorker
import com.apollographql.apollo3.mpp.currentTimeMillis
import com.apollographql.apollo3.network.NetworkTransport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class HttpNetworkTransport @Deprecated("Use HttpNetworkTransport.Builder instead. This will be removed in v3.0.0.") constructor(
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
  @Deprecated("Use HttpNetworkTransport.Builder instead. This will be removed in v3.0.0.") constructor(
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
  @Deprecated("Use HttpNetworkTransport.Builder instead. This will be removed in v3.0.0.") constructor(
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
      val millisStart = currentTimeMillis()
      val httpResponse = DefaultHttpInterceptorChain(
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
                  millisStart = millisStart,
                  millisEnd = currentTimeMillis(),
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
   * Creates a newBuilder that shares the underlying resources
   *
   * Calling [dispose] on the original instance or the new one will terminate the [engine] for both instances
   */
  fun newBuilder(): Builder {
    return Builder()
        .httpEngine(engine)
        .interceptors(interceptors)
        .httpRequestComposer(httpRequestComposer)
  }

  class Builder {
    private var httpRequestComposer: HttpRequestComposer? = null
    private var engine: HttpEngine? = null
    private val interceptors: MutableList<HttpInterceptor> = mutableListOf()

    fun httpRequestComposer(httpRequestComposer: HttpRequestComposer): Builder {
      this.httpRequestComposer = httpRequestComposer
      return this
    }

    fun serverUrl(serverUrl: String): Builder {
      this.httpRequestComposer = DefaultHttpRequestComposer(serverUrl)
      return this
    }

    fun httpHeaders(headers: List<HttpHeader>): Builder {
      interceptors.add(HeadersInterceptor(headers))
      return this
    }

    fun httpEngine(httpEngine: HttpEngine): Builder {
      this.engine = httpEngine
      return this
    }

    fun interceptors(interceptors: List<HttpInterceptor>): Builder {
      this.interceptors.clear()
      this.interceptors.addAll(interceptors)
      return this
    }

    fun addInterceptor(interceptor: HttpInterceptor): Builder {
      this.interceptors.add(interceptor)
      return this
    }

    fun build(): HttpNetworkTransport {
      return HttpNetworkTransport(
          httpRequestComposer = httpRequestComposer ?: error("No HttpRequestComposer found. Use 'httpRequestComposer' or 'serverUrl'"),
          engine = engine ?: DefaultHttpEngine(),
          interceptors = interceptors
      )
    }
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
@Deprecated("Use HttpNetworkTransport.Builder instead. This will be removed in v3.0.0.")
fun HttpNetworkTransport.withDefaultHeaders(headers: List<HttpHeader>) = newBuilder().addInterceptor(HeadersInterceptor(headers))