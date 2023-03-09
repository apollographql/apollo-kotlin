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
import com.apollographql.apollo3.api.withDeferredFragmentIds
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloParseException
import com.apollographql.apollo3.internal.DeferredJsonMerger
import com.apollographql.apollo3.internal.isMultipart
import com.apollographql.apollo3.internal.multipartBodyFlow
import com.apollographql.apollo3.mpp.currentTimeMillis
import com.apollographql.apollo3.network.NetworkTransport
import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import okio.use

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

      emitAll(when {
        httpResponse == null -> {
          flowOf(errorResponse(request.operation, apolloException!!))
        }

        httpResponse.statusCode !in 200..299 -> {
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
          .map {
            it.withHttpInfo(request.requestUuid, httpResponse, millisStart)
          }
      )
    }
  }

  private fun <D : Operation.Data> errorResponse(
      operation: Operation<D>,
      throwable: Throwable,
  ): ApolloResponse<D> {
    val apolloException = if (throwable is ApolloException) {
      throwable
    } else {
      // This happens for null pointer exceptions on missing fields
      ApolloParseException(
          message = "Failed to parse GraphQL http network response",
          cause = throwable
      )
    }
    return ApolloResponse.Builder(requestUuid = uuid4(), operation = operation, data = null)
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
    val response = try {
      operation.parseJsonResponse(
          jsonReader = httpResponse.body!!.jsonReader(),
          customScalarAdapters = customScalarAdapters
      )
    } catch (e: Exception) {
      errorResponse(operation, e)
    }

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
          try {
            val what = part.peek().jsonReader().use { reader ->
              // detect what kind of payload we have
              reader.beginObject()
              if (!reader.hasNext()) {
                return@use EMPTY
              }
              val name = reader.nextName()

              if (name == "payload") {
                return@use PAYLOAD
              } else {
                return@use OTHER
              }
            }

            when (what)  {
              EMPTY -> {
                // nothing to do
                null
              }
              PAYLOAD -> {
                val reader = part.jsonReader()
                // advance the reader
                reader.beginObject()
                reader.nextName()
                operation.parseJsonResponse(
                    jsonReader = reader,
                    customScalarAdapters = customScalarAdapters
                ).newBuilder().build()
              }
              else -> {
                if (jsonMerger == null) {
                  jsonMerger = DeferredJsonMerger()
                }
                val merged = jsonMerger!!.merge(part)
                val deferredFragmentIds = jsonMerger!!.mergedFragmentIds
                val isLast = !jsonMerger!!.hasNext

                if (jsonMerger!!.isEmptyPayload) {
                  null
                } else {
                  operation.parseJsonResponse(
                      jsonReader = merged.jsonReader(),
                      customScalarAdapters = customScalarAdapters.withDeferredFragmentIds(deferredFragmentIds)
                  ).newBuilder().isLast(isLast).build()
                }
              }
            }

          } catch (e: Exception) {
            errorResponse(operation, e)
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

  private companion object {
    private const val EMPTY = 0
    private const val PAYLOAD = 1
    private const val OTHER = 2
  }
}
