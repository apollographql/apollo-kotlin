package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.api.http.HttpRequestComposer
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.api.parseResponseBody
import com.apollographql.apollo3.api.exception.ApolloHttpException
import com.apollographql.apollo3.network.NetworkTransport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ApolloHttpNetworkTransport(
    private val httpRequestComposer: HttpRequestComposer,
    private val engine: HttpEngine,
) : NetworkTransport {

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
      headers: Map<String, String> = emptyMap(),
      connectTimeoutMillis: Long = 60_000,
      readTimeoutMillis: Long = 60_000,
  ) : this(DefaultHttpRequestComposer(serverUrl, headers), DefaultHttpEngine(connectTimeoutMillis, readTimeoutMillis))

  override fun <D : Operation.Data> execute(
      request: ApolloRequest<D>,
  ): Flow<ApolloResponse<D>> {
    val responseAdapterCache = request.executionContext[CustomScalarAdapters]!!

    val httpRequest = httpRequestComposer.compose(request)
    return flow {
      val response = engine.execute(httpRequest)

      val parsedResponse = response.parse(request, responseAdapterCache)

      emit(parsedResponse)
    }
  }

  private fun <D : Operation.Data> HttpResponse.parse(
      request: ApolloRequest<D>,
      customScalarAdapters: CustomScalarAdapters,
  ): ApolloResponse<D> {
    if (statusCode !in 200..299) {
      throw ApolloHttpException(
          statusCode = statusCode,
          headers = headers,
          message = "Http request failed with status code `${statusCode} (${body?.readUtf8()})`"
      )
    }

    return request.operation.parseResponseBody(
        source = body!!,
        customScalarAdapters = customScalarAdapters
    ).copy(
        requestUuid = request.requestUuid,
        executionContext = request.executionContext + HttpResponseInfo(
            statusCode = statusCode,
            headers = headers
        )
    )
  }
}
