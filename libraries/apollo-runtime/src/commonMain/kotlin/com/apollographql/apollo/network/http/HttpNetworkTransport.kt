package com.apollographql.apollo.network.http

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpRequestComposer
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.api.internal.readErrors
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.parseResponse
import com.apollographql.apollo.api.toApolloResponse
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.RouterError
import com.apollographql.apollo.internal.DeferredJsonMerger
import com.apollographql.apollo.internal.isGraphQLResponse
import com.apollographql.apollo.internal.isMultipart
import com.apollographql.apollo.internal.multipartBodyFlow
import com.apollographql.apollo.mpp.currentTimeMillis
import com.apollographql.apollo.network.NetworkTransport
import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class HttpNetworkTransport
private constructor(
    private val httpRequestComposer: HttpRequestComposer,
    private val engine: HttpEngine,
    val interceptors: List<HttpInterceptor>,
    private val exposeErrorBody: Boolean,
) : NetworkTransport {
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
      var apolloException: ApolloException? = null
      val httpResponse: HttpResponse? = try {
        DefaultHttpInterceptorChain(
            interceptors = interceptors + engineInterceptor,
            index = 0
        ).proceed(httpRequest)
      } catch (e: ApolloException) {
        apolloException = e
        null
      }

      val responses = when {
        httpResponse == null -> {
          flowOf(errorResponse(request.operation, apolloException!!))
        }

        httpResponse.statusCode !in 200..299 && !httpResponse.isGraphQLResponse -> {
          /*
           * application/json may contain something else than a GraphQL response.
           * Typically, this happens if a proxy/other non-GraphQL intermediary encounters an error.
           *
           * In those cases, don't try to parse the body
           * See https://graphql.github.io/graphql-over-http/draft/#sec-Processing-the-response
           */
          errorResponse(request.operation, httpResponse)
        }

        // When using @defer, the response contains multiple parts, using the multipart content type.
        // See https://github.com/graphql/graphql-over-http/blob/main/rfcs/IncrementalDelivery.md
        httpResponse.isMultipart -> {
          multipleResponses(request.operation, customScalarAdapters, httpResponse)
        }

        else -> {
          singleResponse(request.operation, customScalarAdapters, httpResponse)
        }
      }

      emitAll(responses.map {
        it.withHttpInfo(request.requestUuid, httpResponse, millisStart)
      })
    }
  }

  private fun <D : Operation.Data> errorResponse(
      operation: Operation<D>,
      throwable: Throwable,
  ): ApolloResponse<D> {
    val apolloException = if (throwable is ApolloException) {
      throwable
    } else {
      ApolloNetworkException(
          message = "Error while reading JSON response",
          platformCause = throwable
      )
    }
    return ApolloResponse.Builder(requestUuid = uuid4(), operation = operation)
        .exception(apolloException)
        .isLast(true)
        .build()
  }

  private fun <D : Operation.Data> errorResponse(
      operation: Operation<D>,
      httpResponse: HttpResponse,
  ): Flow<ApolloResponse<D>> {
    val maybeBody = if (exposeErrorBody) {
      httpResponse.body
    } else {
      httpResponse.body?.close()
      null
    }
    val apolloException = ApolloHttpException(
        statusCode = httpResponse.statusCode,
        headers = httpResponse.headers,
        body = maybeBody,
        message = "Http request failed with status code `${httpResponse.statusCode}`"
    )
    return flowOf(errorResponse(operation, apolloException))
  }

  private fun <D : Operation.Data> singleResponse(
      operation: Operation<D>,
      customScalarAdapters: CustomScalarAdapters,
      httpResponse: HttpResponse,
  ): Flow<ApolloResponse<D>> {
    val response = httpResponse.body!!.jsonReader().toApolloResponse(
        operation,
        customScalarAdapters = customScalarAdapters,
        deferredFragmentIdentifiers = null,
    )

    return flowOf(response.newBuilder().isLast(true).build())
  }

  private fun <D : Operation.Data> multipleResponses(
      operation: Operation<D>,
      customScalarAdapters: CustomScalarAdapters,
      httpResponse: HttpResponse,
  ): Flow<ApolloResponse<D>> {
    var jsonMerger: DeferredJsonMerger? = null

    return multipartBodyFlow(httpResponse)
        .mapNotNull { part ->
          if (operation is Subscription) {
            val reader = part.jsonReader()
            var payloadResponse: ApolloResponse<D>? = null
            var errors: List<Error>? = null
            reader.beginObject()
            while (reader.hasNext()) {
              when (reader.nextName()) {
                "payload" -> {
                  if (reader.peek() == JsonReader.Token.NULL) {
                    reader.skipValue()
                  } else {
                    payloadResponse = reader.parseResponse(
                        operation = operation,
                        customScalarAdapters = customScalarAdapters,
                        deferredFragmentIdentifiers = null
                    )
                  }
                }

                "errors" -> {
                  if (reader.peek() == JsonReader.Token.NULL) {
                    reader.skipValue()
                  } else {
                    errors = reader.readErrors()
                  }
                }

                else -> {
                  // Ignore unknown keys
                  reader.skipValue()
                }
              }
            }
            reader.endObject()
            when {
              errors != null -> {
                errorResponse(operation, RouterError(errors))
              }

              payloadResponse != null -> payloadResponse
              else -> null
            }
          } else {
            if (jsonMerger == null) {
              jsonMerger = DeferredJsonMerger()
            }
            val merged = jsonMerger!!.merge(part)
            val deferredFragmentIds = jsonMerger!!.mergedFragmentIds
            val isLast = !jsonMerger!!.hasNext

            if (jsonMerger!!.isEmptyPayload) {
              null
            } else {
              @Suppress("DEPRECATION")
              merged.jsonReader().toApolloResponse(
                  operation = operation,
                  customScalarAdapters = customScalarAdapters,
                  deferredFragmentIdentifiers = deferredFragmentIds
              ).newBuilder().isLast(isLast).build()
            }
          }
        }.catch { throwable ->
          if (throwable is ApolloException) {
            emit(
                ApolloResponse.Builder(operation = operation, requestUuid = uuid4())
                    .exception(throwable)
                    .build()
            )
          }
        }
  }

  private fun <D : Operation.Data> ApolloResponse<D>.withHttpInfo(
      requestUuid: Uuid,
      httpResponse: HttpResponse?,
      millisStart: Long,
  ) = newBuilder()
      .requestUuid(requestUuid)
      .apply {
        if (httpResponse != null) {
          addExecutionContext(
              @Suppress("DEPRECATION")
              HttpInfo(
                  startMillis = millisStart,
                  endMillis = currentTimeMillis(),
                  statusCode = httpResponse.statusCode,
                  headers = httpResponse.headers
              )
          )
        }
      }
      .build()

  inner class EngineInterceptor : HttpInterceptor {
    override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
      return engine.execute(request)
    }
  }

  override fun dispose() {
    interceptors.forEach { it.dispose() }
    engine.close()
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
        .exposeErrorBody(exposeErrorBody)
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
    private val headers: MutableList<HttpHeader> = mutableListOf()

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

    @Deprecated("Use ApolloClient.Builder.addHttpHeader() instead")
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
    fun addHttpHeader(name: String, value: String) = apply {
      headers.add(HttpHeader(name, value))
    }

    @Deprecated("Use ApolloClient.Builder.httpHeader() instead")
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
    fun httpHeaders(headers: List<HttpHeader>) = apply {
      // In case this builder comes from newBuilder(), remove any existing interceptor
      interceptors.removeAll {
        it is TransportHeadersInterceptor
      }
      this.headers.clear()
      this.headers.addAll(headers)
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

      if (headers.isNotEmpty()) {
        interceptors.add(TransportHeadersInterceptor(headers))
      }

      return HttpNetworkTransport(
          httpRequestComposer = composer,
          engine = engine ?: DefaultHttpEngine(),
          interceptors = interceptors,
          exposeErrorBody = exposeErrorBody,
      )
    }
  }

  private class TransportHeadersInterceptor(private val headers: List<HttpHeader>) : HttpInterceptor {
    override suspend fun intercept(
        request: HttpRequest,
        chain: HttpInterceptorChain,
    ): HttpResponse {
      return chain.proceed(request.newBuilder().addHeaders(headers).build())
    }
  }


  private companion object {
    enum class Kind {
      EMPTY,
      PAYLOAD,
      OTHER,
    }
  }
}
