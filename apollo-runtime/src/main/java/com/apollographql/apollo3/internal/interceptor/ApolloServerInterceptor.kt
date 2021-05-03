package com.apollographql.apollo3.internal.interceptor

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.CustomScalarAdpaters
import com.apollographql.apollo3.api.cache.http.HttpCache
import com.apollographql.apollo3.api.cache.http.HttpCachePolicy
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.api.http.HttpRequestComposerParams
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.internal.ApolloLogger
import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.cache.ApolloCacheHeaders
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptor.CallBack
import com.apollographql.apollo3.interceptor.ApolloInterceptor.FetchSourceType
import com.apollographql.apollo3.interceptor.ApolloInterceptor.InterceptorRequest
import com.apollographql.apollo3.interceptor.ApolloInterceptor.InterceptorResponse
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.request.RequestHeaders
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import okio.BufferedSink
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference

/**
 * ApolloServerInterceptor is a concrete [ApolloInterceptor] responsible for making the network calls to the
 * server. It is the last interceptor in the chain of interceptors and hence doesn't call
 * [ApolloInterceptorChain.proceedAsync]
 * on the interceptor chain.
 */
class ApolloServerInterceptor(
    private val serverUrl: HttpUrl,
    private val httpCallFactory: Call.Factory,
    private val cachePolicy: HttpCachePolicy.Policy?,
    val prefetch: Boolean,
    val responseAdapterCache: CustomScalarAdpaters,
    val logger: ApolloLogger,
) : ApolloInterceptor {
  var httpCallRef = AtomicReference<Call?>()

  @Volatile
  var disposed = false

  val composer = DefaultHttpRequestComposer(serverUrl.toString())

  override fun interceptAsync(
      request: InterceptorRequest, chain: ApolloInterceptorChain,
      dispatcher: Executor, callBack: CallBack,
  ) {
    dispatcher.execute { executeHttpCall(request, callBack) }
  }

  override fun dispose() {
    disposed = true
    val httpCall = httpCallRef.getAndSet(null)
    httpCall?.cancel()
  }

  fun executeHttpCall(request: InterceptorRequest, callBack: CallBack) {
    if (disposed) return
    callBack.onFetch(FetchSourceType.NETWORK)
    val httpCall: Call
    httpCall = try {
      if (request.useHttpGetMethodForQueries && request.operation is Query<*>) {
        httpGetCall(request.operation, request.cacheHeaders, request.requestHeaders,
            request.sendQueryDocument, request.autoPersistQueries)
      } else {
        httpPostCall(request.operation, request.cacheHeaders, request.requestHeaders,
            request.sendQueryDocument, request.autoPersistQueries)
      }
    } catch (e: IOException) {
      logger.e(e, "Failed to prepare http call for operation %s", request.operation.name())
      callBack.onFailure(ApolloNetworkException("Failed to prepare http call", e))
      return
    }
    val previousCall = httpCallRef.getAndSet(httpCall)
    previousCall?.cancel()
    if (httpCall.isCanceled || disposed) {
      httpCallRef.compareAndSet(httpCall, null)
      return
    }
    httpCall.enqueue(object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        if (disposed) return
        if (httpCallRef.compareAndSet(httpCall, null)) {
          logger.e(e, "Failed to execute http call for operation %s", request.operation.name())
          callBack.onFailure(ApolloNetworkException("Failed to execute http call", e))
        }
      }

      override fun onResponse(call: Call, response: Response) {
        if (disposed) return
        if (httpCallRef.compareAndSet(httpCall, null)) {
          callBack.onResponse(InterceptorResponse(response))
          callBack.onCompleted()
        }
      }
    })
  }

  @Throws(IOException::class)
  fun httpGetCall(
      operation: Operation<*>, cacheHeaders: CacheHeaders, requestHeaders: RequestHeaders,
      writeQueryDocument: Boolean, autoPersistQueries: Boolean,
  ): Call {
    val requestBuilder = Request.Builder()
        .url(httpGetUrl(serverUrl, operation, responseAdapterCache, writeQueryDocument, autoPersistQueries))
        .get()
    decorateRequest(requestBuilder, operation, cacheHeaders, requestHeaders)
    return httpCallFactory.newCall(requestBuilder.build())
  }

  @Throws(IOException::class)
  fun httpPostCall(
      operation: Operation<*>, cacheHeaders: CacheHeaders, requestHeaders: RequestHeaders,
      writeQueryDocument: Boolean, autoPersistQueries: Boolean,
  ): Call {
    val body = composer.compose(ApolloRequest(operation)
        .withExecutionContext(responseAdapterCache)
        .withExecutionContext(
            HttpRequestComposerParams(
                method = HttpMethod.Post,
                autoPersistQueries = autoPersistQueries,
                sendDocument = writeQueryDocument,
                extraHeaders = emptyMap()
            )
        )
    ).body!!

    val requestBody = object : RequestBody() {
      override fun contentType() = MediaType.parse(body.contentType)

      override fun contentLength() = body.contentLength

      override fun writeTo(sink: BufferedSink) {
        body.writeTo(bufferedSink = sink)
      }
    }
    val requestBuilder = Request.Builder()
        .url(serverUrl)
        .post(requestBody)
    decorateRequest(requestBuilder, operation, cacheHeaders, requestHeaders)
    return httpCallFactory.newCall(requestBuilder.build())
  }

  @Throws(IOException::class)
  fun decorateRequest(
      requestBuilder: Request.Builder, operation: Operation<*>, cacheHeaders: CacheHeaders,
      requestHeaders: RequestHeaders,
  ) {
    requestBuilder
        .header(HEADER_ACCEPT_TYPE, JSON_CONTENT_TYPE)
        /**
         * Content-Type is usually taken from the RequestBody but some implementation might not use OkHttp as a CallFactory
         * and therefore use this
         */
        .header(HEADER_CONTENT_TYPE, JSON_CONTENT_TYPE)
        .header(HEADER_APOLLO_OPERATION_ID, operation.id())
        .header(HEADER_APOLLO_OPERATION_NAME, operation.name())
        .tag(operation.id())
    for (header in requestHeaders.headers()) {
      val value = requestHeaders.headerValue(header)
      requestBuilder.header(header, value)
    }
    if (cachePolicy != null) {
      val skipCacheHttpResponse = "true".equals(cacheHeaders.headerValue(
          ApolloCacheHeaders.DO_NOT_STORE), ignoreCase = true)
      val cacheKey = cacheKey(operation, responseAdapterCache)
      requestBuilder
          .header(HttpCache.CACHE_KEY_HEADER, cacheKey)
          .header(HttpCache.CACHE_FETCH_STRATEGY_HEADER, cachePolicy.fetchStrategy.name)
          .header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER, cachePolicy.expireTimeoutMs().toString())
          .header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER, java.lang.Boolean.toString(cachePolicy.expireAfterRead))
          .header(HttpCache.CACHE_PREFETCH_HEADER, java.lang.Boolean.toString(prefetch))
          .header(HttpCache.CACHE_DO_NOT_STORE, java.lang.Boolean.toString(skipCacheHttpResponse))
    }
  }

  companion object {
    const val HEADER_ACCEPT_TYPE = "Accept"
    const val HEADER_CONTENT_TYPE = "Content-Type"
    const val HEADER_APOLLO_OPERATION_ID = "X-APOLLO-OPERATION-ID"
    const val HEADER_APOLLO_OPERATION_NAME = "X-APOLLO-OPERATION-NAME"
    const val JSON_CONTENT_TYPE = "application/json"

    @Throws(IOException::class)
    fun cacheKey(operation: Operation<*>, responseAdapterCache: CustomScalarAdpaters): String {
      return DefaultHttpRequestComposer.buildParamsMap(
          operation = operation,
          autoPersistQueries = true,
          sendDocument = true,
          responseAdapterCache = responseAdapterCache
      ).md5().hex()
    }

    @Throws(IOException::class)
    fun httpGetUrl(
        serverUrl: HttpUrl, operation: Operation<*>,
        responseAdapterCache: CustomScalarAdpaters?, writeQueryDocument: Boolean,
        autoPersistQueries: Boolean,
    ): HttpUrl {
      val urlBuilder = serverUrl.newBuilder()
      if (!autoPersistQueries || writeQueryDocument) {
        urlBuilder.addQueryParameter("query", operation.document())
      }
      addVariablesUrlQueryParameter(urlBuilder, operation, responseAdapterCache)

      urlBuilder.addQueryParameter("operationName", operation.name())
      if (autoPersistQueries) {
        addExtensionsUrlQueryParameter(urlBuilder, operation)
      }
      return urlBuilder.build()
    }

    @Throws(IOException::class)
    fun addVariablesUrlQueryParameter(
        urlBuilder: HttpUrl.Builder,
        operation: Operation<*>,
        responseAdapterCache: CustomScalarAdpaters?,
    ) {
      val buffer = Buffer()
      val jsonWriter = BufferedSinkJsonWriter(buffer)
      jsonWriter.beginObject()
      operation.serializeVariables(jsonWriter, responseAdapterCache!!)
      jsonWriter.endObject()
      jsonWriter.close()
      urlBuilder.addQueryParameter("variables", buffer.readUtf8())
    }

    @Throws(IOException::class)
    fun addExtensionsUrlQueryParameter(urlBuilder: HttpUrl.Builder, operation: Operation<*>) {
      val buffer = Buffer()
      val jsonWriter = BufferedSinkJsonWriter(buffer)
      jsonWriter.beginObject()
      jsonWriter.name("persistedQuery")
          .beginObject()
          .name("version").value(1)
          .name("sha256Hash").value(operation.id())
          .endObject()
      jsonWriter.endObject()
      jsonWriter.close()
      urlBuilder.addQueryParameter("extensions", buffer.readUtf8())
    }
  }
}
