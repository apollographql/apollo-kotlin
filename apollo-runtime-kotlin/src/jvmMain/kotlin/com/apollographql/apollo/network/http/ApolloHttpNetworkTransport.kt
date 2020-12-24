package com.apollographql.apollo.network.http

import com.apollographql.apollo.ApolloException
import com.apollographql.apollo.ApolloHttpException
import com.apollographql.apollo.ApolloNetworkException
import com.apollographql.apollo.ApolloParseException
import com.apollographql.apollo.ApolloSerializationException
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.interceptor.ApolloRequest
import com.apollographql.apollo.interceptor.ApolloResponse
import com.apollographql.apollo.network.HttpExecutionContext
import com.apollographql.apollo.network.HttpMethod
import com.apollographql.apollo.network.NetworkTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val MEDIA_TYPE = "application/json; charset=utf-8"

@ApolloExperimental
@ExperimentalCoroutinesApi
actual class ApolloHttpNetworkTransport(
    private val serverUrl: HttpUrl,
    private val headers: Headers,
    private val httpCallFactory: Call.Factory,
    private val httpMethod: HttpMethod,
) : NetworkTransport {

  actual constructor(
      serverUrl: String,
      headers: Map<String, String>,
      httpMethod: HttpMethod,
      connectTimeoutMillis: Long,
      readTimeoutMillis: Long
  ) : this(
      serverUrl = serverUrl.toHttpUrl(),
      headers = headers.toHeaders(),
      httpCallFactory = OkHttpClient.Builder()
          .connectTimeout(connectTimeoutMillis, TimeUnit.MILLISECONDS)
          .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
          .build(),
      httpMethod = httpMethod,
  )

  override fun <D : Operation.Data> execute(request: ApolloRequest<D>, executionContext: ExecutionContext): Flow<ApolloResponse<D>> {
    return flow {
      val response = suspendCancellableCoroutine<ApolloResponse<D>> { continuation ->
        val httpRequest = request.toHttpRequest(executionContext[HttpExecutionContext.Request])
        httpCallFactory.newCall(httpRequest)
            .also { call ->
              continuation.invokeOnCancellation {
                call.cancel()
              }
            }
            .enqueue(
                object : Callback {
                  override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isCancelled) return
                    continuation.resumeWithException(
                        ApolloNetworkException(
                            message = "Failed to execute GraphQL http network request",
                            cause = e
                        )
                    )
                  }

                  override fun onResponse(call: Call, response: Response) {
                    if (continuation.isCancelled) return
                    runCatching { response.parse(request) }
                        .onSuccess { graphQlResponse -> continuation.resume(graphQlResponse) }
                        .onFailure { e ->
                          response.closeQuietly()
                          if (e is ApolloException) {
                            continuation.resumeWithException(e)
                          } else {
                            continuation.resumeWithException(
                                ApolloParseException(
                                    message = "Failed to parse GraphQL http network response",
                                    cause = e
                                )
                            )
                          }
                        }
                  }
                }
            )
      }
      emit(response)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun <D : Operation.Data> Response.parse(request: ApolloRequest<D>): ApolloResponse<D> {
    if (!isSuccessful) throw ApolloHttpException(
        statusCode = code,
        headers = headers.toMap(),
        message = "Http request failed with status code `$code ($message)`"
    )

    val responseBody = body ?: throw ApolloHttpException(
        statusCode = code,
        headers = headers.toMap(),
        message = "Failed to parse GraphQL http network response: EOF"
    )

    val response = request.operation.parse(
        source = responseBody.source(),
        customScalarAdapters = request.customScalarAdapters
    )
    return ApolloResponse(
        requestUuid = request.requestUuid,
        response = response,
        executionContext = request.executionContext + HttpExecutionContext.Response(
            statusCode = code,
            headers = headers.toMap()
        )
    )
  }

  private fun <D : Operation.Data> ApolloRequest<D>.toHttpRequest(httpExecutionContext: HttpExecutionContext.Request?): Request {
    try {
      return when (httpMethod) {
        HttpMethod.Get -> toHttpGetRequest(httpExecutionContext)
        HttpMethod.Post -> toHttpPostRequest(httpExecutionContext)
      }
    } catch (e: Exception) {
      throw ApolloSerializationException(
          message = "Failed to compose GraphQL network request",
          cause = e
      )
    }
  }

  private fun <D : Operation.Data> ApolloRequest<D>.toHttpGetRequest(httpExecutionContext: HttpExecutionContext.Request?): Request {
    val url = serverUrl.newBuilder()
        .addQueryParameter("query", operation.queryDocument())
        .addQueryParameter("operationName", operation.name().name())
        .apply {
          operation.variables().marshal(customScalarAdapters).let { variables ->
            if (variables.isNotEmpty()) addQueryParameter("variables", variables)
          }
        }
        .build()
    return Request.Builder()
        .url(url)
        .headers(headers)
        .apply {
          httpExecutionContext?.headers?.forEach { (name, value) ->
            header(name = name, value = value)
          }
        }
        .build()
  }

  private fun <D : Operation.Data> ApolloRequest<D>.toHttpPostRequest(httpExecutionContext: HttpExecutionContext.Request?): Request {
    val requestBody = operation.composeRequestBody(customScalarAdapters).toRequestBody(contentType = MEDIA_TYPE.toMediaType())
    return Request.Builder()
        .url(serverUrl)
        .headers(headers)
        .apply {
          httpExecutionContext?.headers?.forEach { (name, value) ->
            header(name = name, value = value)
          }
        }
        .post(requestBody)
        .build()
  }
}
