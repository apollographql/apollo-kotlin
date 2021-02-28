package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.ApolloRequest
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.fromResponse
import com.apollographql.apollo3.api.internal.OperationRequestBodyComposer
import com.apollographql.apollo3.api.variablesJson
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloSerializationException
import com.apollographql.apollo3.network.NetworkTransport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okio.BufferedSink

class ApolloHttpNetworkTransport(
    private val serverUrl: String,
    private val httpMethod: HttpMethod = HttpMethod.Post,
    private val headers: Map<String, String> = emptyMap(),

    /**
     * The timeout interval to use when connecting
     *
     * - on iOS, it is used to set [NSMutableURLRequest.timeoutInterval]
     * - on Android, it is used to set [OkHttpClient.connectTimeout]
     */
    connectTimeoutMillis: Long = 60_000,
    /**
     * The timeout interval to use when waiting for additional data.
     *
     * - on iOS, it is used to set [NSURLSessionConfiguration.timeoutIntervalForRequest]
     * - on Android, it is used to set  [OkHttpClient.readTimeout]
     */
    readTimeoutMillis: Long = 60_000,

    private val engine: HttpEngine = DefaultHttpEngine(connectTimeoutMillis, readTimeoutMillis)
) : NetworkTransport {
    override fun <D : Operation.Data> execute(request: ApolloRequest<D>, responseAdapterCache: ResponseAdapterCache): Flow<ApolloResponse<D>> {
        val httpRequest = request.toHttpRequest(responseAdapterCache)
        return flow {
            emit(engine.execute(httpRequest) {
                it.parse(request, responseAdapterCache)
            })
        }
    }

    private fun <D : Operation.Data> HttpResponse.parse(
        request: ApolloRequest<D>,
        responseAdapterCache: ResponseAdapterCache
    ): ApolloResponse<D> {
        if (statusCode !in 200..299) {
            throw ApolloHttpException(
                statusCode = statusCode,
                headers = headers,
                message = "Http request failed with status code `${statusCode} (${body?.readUtf8()})`"
            )
        }

        return request.operation.fromResponse(
            source = body!!,
            responseAdapterCache = responseAdapterCache
        ).copy(
            requestUuid = request.requestUuid,
            executionContext = request.executionContext + HttpResponseInfo(
                statusCode = statusCode,
                headers = headers
            )
        )
    }

    private fun <D : Operation.Data> ApolloRequest<D>.toHttpRequest(
        responseAdapterCache: ResponseAdapterCache
    ): HttpRequest {
        try {
            return when (httpMethod) {
                HttpMethod.Get -> toHttpGetRequest(responseAdapterCache)
                HttpMethod.Post -> toHttpPostRequest(responseAdapterCache)
            }
        } catch (e: Exception) {
            throw ApolloSerializationException(
                message = "Failed to compose GraphQL network request",
                cause = e
            )
        }
    }

    private fun <D : Operation.Data> ApolloRequest<D>.toHttpGetRequest(
        responseAdapterCache: ResponseAdapterCache
    ): HttpRequest {
        val url = buildUrl(serverUrl, mapOf(
            "query" to operation.queryDocument(),
            "operationName" to operation.name(),
            "variables" to operation.variablesJson(responseAdapterCache)
        ))

        return HttpRequest(
            method = HttpMethod.Get,
            url =  url,
            headers = headers + (executionContext[HttpRequestParameters]?.headers ?: emptyMap()),
            body = null
        )
    }

    private fun <D : Operation.Data> ApolloRequest<D>.toHttpPostRequest(
        responseAdapterCache: ResponseAdapterCache
    ): HttpRequest {
        val requestBody = OperationRequestBodyComposer.compose(
            operation = operation,
            autoPersistQueries = false,
            withQueryDocument = true,
            responseAdapterCache = responseAdapterCache
        )

        return HttpRequest(
            method = HttpMethod.Post,
            url = serverUrl,
            headers = headers + (executionContext[HttpRequestParameters]?.headers ?: emptyMap()),
            body = object : HttpBody {
                override val contentType = requestBody.contentType
                override val contentLength = -1L

                override fun writeTo(bufferedSink: BufferedSink) {
                    requestBody.writeTo(bufferedSink)
                }
            }
        )
    }

}
