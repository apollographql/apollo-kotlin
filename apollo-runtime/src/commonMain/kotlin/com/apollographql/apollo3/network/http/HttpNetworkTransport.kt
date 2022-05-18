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
import com.apollographql.apollo3.internal.NonMainWorker
import com.apollographql.apollo3.internal.deepCopy
import com.apollographql.apollo3.internal.isMultipart
import com.apollographql.apollo3.internal.multipartBodyFlow
import com.apollographql.apollo3.mpp.Platform
import com.apollographql.apollo3.mpp.currentTimeMillis
import com.apollographql.apollo3.mpp.platform
import com.apollographql.apollo3.network.NetworkTransport
import com.benasher44.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

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
      if (httpResponse.isMultipart) {
        emitAll(
            multipleResponses(request.operation, customScalarAdapters, httpResponse)
                .map { it.withHttpInfo(request.requestUuid, httpResponse, millisStart) }
        )
      } else {
        emit(
            singleResponse(request.operation, customScalarAdapters, httpResponse)
                .withHttpInfo(request.requestUuid, httpResponse, millisStart)
        )
      }
    }
  }

  private suspend fun <D : Operation.Data> singleResponse(
      operation: Operation<D>,
      customScalarAdapters: CustomScalarAdapters,
      httpResponse: HttpResponse,
  ): ApolloResponse<D> {
    val response = try {
      worker.doWork {
        operation.parseJsonResponse(
            jsonReader = httpResponse.body!!.jsonReader(),
            customScalarAdapters = customScalarAdapters
        )
      }
    } catch (e: Exception) {
      throw wrapThrowableIfNeeded(e)
    }

    return response.newBuilder().isLast(true).build()
  }

  private suspend fun <D : Operation.Data> multipleResponses(
      operation: Operation<D>,
      customScalarAdapters: CustomScalarAdapters,
      httpResponse: HttpResponse,
  ): Flow<ApolloResponse<D>> {
    val jsonMerger = DeferredJsonMerger()
    return multipartBodyFlow(httpResponse).map { part ->
      try {
        // On native, we cannot pass `jsonMerger._merge` to `worker.doWork` or it will become frozen making
        // any subsequent payload merge throw.
        // So we clone them before they are captured.
        // XXX: revisit with the new memory model
        val mustClone = platform() == Platform.Native
        val merged = jsonMerger.merge(part).let { if (mustClone) it.deepCopy() else it }
        val deferredFragmentIds = jsonMerger.mergedFragmentIds.let { if (mustClone) it.toSet() else it }
        val isLast = !jsonMerger.hasNext
        worker.doWork {
          operation.parseJsonResponse(
              jsonReader = merged.jsonReader(),
              customScalarAdapters = customScalarAdapters.withDeferredFragmentIds(deferredFragmentIds)
          ).newBuilder().isLast(isLast).build()
        }
      } catch (e: Exception) {
        throw wrapThrowableIfNeeded(e)
      }
    }
  }

  private fun <D : Operation.Data> ApolloResponse<D>.withHttpInfo(
      requestUuid: Uuid,
      httpResponse: HttpResponse,
      millisStart: Long,
  ) = newBuilder()
      .requestUuid(requestUuid)
      .addExecutionContext(
          @Suppress("DEPRECATION")
          HttpInfo(
              startMillis = millisStart,
              endMillis = currentTimeMillis(),
              statusCode = httpResponse.statusCode,
              headers = httpResponse.headers
          )
      )
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
