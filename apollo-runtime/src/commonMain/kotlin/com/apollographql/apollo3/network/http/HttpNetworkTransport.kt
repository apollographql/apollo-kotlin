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
import com.apollographql.apollo3.internal.isMultipart
import com.apollographql.apollo3.internal.multipartBodyFlow
import com.apollographql.apollo3.mpp.Platform
import com.apollographql.apollo3.mpp.currentTimeMillis
import com.apollographql.apollo3.mpp.platform
import com.apollographql.apollo3.network.NetworkTransport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import okio.Buffer
import okio.BufferedSource

class HttpNetworkTransport
private constructor(
    private val httpRequestComposer: HttpRequestComposer,
    val engine: HttpEngine,
    val interceptors: List<HttpInterceptor>,
    val exposeErrorBody: Boolean,
) : NetworkTransport {
  private val worker = NonMainWorker()

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
        val maybeBody = if (exposeErrorBody) {
          httpResponse.body
        } else {
          httpResponse.body?.close()
          null
        }
        throw ApolloHttpException(
            statusCode = httpResponse.statusCode,
            headers = httpResponse.headers,
            body = maybeBody,
            message = "Http request failed with status code `${httpResponse.statusCode}`"
        )
      }

      // When using @defer, the response contains multiple parts, using the multipart content type.
      // See https://github.com/graphql/graphql-over-http/blob/main/rfcs/IncrementalDelivery.md
      val bodies: Flow<BufferedSource> = if (httpResponse.isMultipart) {
        multipartBodyFlow(httpResponse)
      } else {
        flowOf(httpResponse.body!!)
      }

      emitAll(
          bodies.map { body ->
            // On native, body will be frozen in worker.doWork, but reading a BufferedSource is a mutating operation.
            // So we read the bytes into a ByteString first, and create a new BufferedSource from it after freezing.
            val bodyByteString = if (platform() == Platform.Native) body.readByteString() else null
            // Do not capture request
            val operation = request.operation
            val response = try {
              if (bodyByteString != null) {
                // Native case, use bodyByteString
                worker.doWork {
                  operation.parseJsonResponse(
                      jsonReader = Buffer().apply { write(bodyByteString) }.jsonReader(),
                      customScalarAdapters = customScalarAdapters
                  )
                }
              } else {
                // Non-native case, use body
                worker.doWork {
                  operation.parseJsonResponse(
                      jsonReader = body.jsonReader(),
                      customScalarAdapters = customScalarAdapters
                  )
                }
              }
            } catch (e: Exception) {
              throw wrapThrowableIfNeeded(e)
            }

            @Suppress("DEPRECATION")
            response.newBuilder()
                .requestUuid(request.requestUuid)
                .addExecutionContext(
                    HttpInfo(
                        startMillis = millisStart,
                        endMillis = currentTimeMillis(),
                        statusCode = httpResponse.statusCode,
                        headers = httpResponse.headers
                    )
                )
                .isLast(true)
                .build()
          }
      )
    }
  }

  inner class EngineInterceptor : HttpInterceptor {
    override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
      return engine.execute(request)
    }
  }

  override fun dispose() {
    interceptors.forEach { it.dispose() }
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

  /**
   * A builder for [HttpNetworkTransport]
   */
  class Builder {
    private var httpRequestComposer: HttpRequestComposer? = null
    private var serverUrl: String? = null
    private var engine: HttpEngine? = null
    private val interceptors: MutableList<HttpInterceptor> = mutableListOf()
    private var exposeErrorBody: Boolean = false

    fun httpRequestComposer(httpRequestComposer: HttpRequestComposer) = apply {
      this.httpRequestComposer = httpRequestComposer
    }

    fun serverUrl(serverUrl: String) = apply {
      this.serverUrl = serverUrl
    }

    /**
     * Configures whether to expose the error body in [ApolloHttpException].
     *
     * If you're setting this to `true`, you **must** catch [ApolloHttpException] and close the body explicitly
     * to avoid sockets and other resources leaking.
     *
     * Default: false
     */
    fun exposeErrorBody(exposeErrorBody: Boolean) = apply {
      this.exposeErrorBody = exposeErrorBody
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
      check(httpRequestComposer == null || serverUrl == null) {
        "It is an error to set both 'httpRequestComposer' and 'serverUrl'"
      }
      val composer = httpRequestComposer
          ?: serverUrl?.let { DefaultHttpRequestComposer(it) }
          ?: error("No HttpRequestComposer found. Use 'httpRequestComposer' or 'serverUrl'")
      return HttpNetworkTransport(
          httpRequestComposer = composer,
          engine = engine ?: DefaultHttpEngine(),
          interceptors = interceptors,
          exposeErrorBody = exposeErrorBody,
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
