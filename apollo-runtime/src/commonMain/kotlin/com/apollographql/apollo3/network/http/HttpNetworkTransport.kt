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
import com.apollographql.apollo3.api.json.jsonReader
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
   * @param timeoutMillis The timeout interval to use when connecting or when waiting for additional data.
   *
   * - on iOS, it is used to set [NSMutableURLRequest.timeoutInterval]
   * - on Android, it is used to set [OkHttpClient.connectTimeout] and  [OkHttpClient.readTimeout]
   */
  @Suppress("DEPRECATION", "DEPRECATION")
  @Deprecated("Use HttpNetworkTransport.Builder instead. This will be removed in v3.0.0.")
  constructor(
      serverUrl: String,
      timeoutMillis: Long = 60_000,
      interceptors: List<HttpInterceptor> = emptyList(),
  ) : this(
      DefaultHttpRequestComposer(serverUrl),
      DefaultHttpEngine(timeoutMillis),
      interceptors
  )

  @Suppress("DEPRECATION")
  @Deprecated("Use HttpNetworkTransport.Builder instead. This will be removed in v3.0.0.")
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
              jsonReader = httpResponse.body!!.jsonReader(),
              customScalarAdapters = customScalarAdapters
          )
        } catch (e: Exception) {
          throw wrapThrowableIfNeeded(e)
        }
      }

      emit(
          response.newBuilder()
              .requestUuid(request.requestUuid)
              .addExecutionContext(
                  HttpInfo(
                      millisStart = millisStart,
                      millisEnd = currentTimeMillis(),
                      statusCode = httpResponse.statusCode,
                      headers = httpResponse.headers
                  )
              )
              .build()
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
   * Creates a new Builder that shares the underlying resources
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

    fun httpRequestComposer(httpRequestComposer: HttpRequestComposer) = apply {
      this.httpRequestComposer = httpRequestComposer
    }

    fun serverUrl(serverUrl: String) = apply {
      this.httpRequestComposer = DefaultHttpRequestComposer(serverUrl)
    }

    fun httpHeaders(headers: List<HttpHeader>) = apply {
      interceptors.add(HeadersInterceptor(headers))
    }

    fun httpEngine(httpEngine: HttpEngine) = apply {
      this.engine = httpEngine
    }

    fun interceptors(interceptors: List<HttpInterceptor>) = apply {
      this.interceptors.clear()
      this.interceptors.addAll(interceptors)
    }

    fun addInterceptor(interceptor: HttpInterceptor) = apply {
      this.interceptors.add(interceptor)
    }

    fun build(): HttpNetworkTransport {
      @Suppress("DEPRECATION")
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