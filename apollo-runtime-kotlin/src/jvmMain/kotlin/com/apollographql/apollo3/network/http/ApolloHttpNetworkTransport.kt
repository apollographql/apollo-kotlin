package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.exception.ApolloParseException
import com.apollographql.apollo3.exception.ApolloSerializationException
import com.apollographql.apollo3.api.ApolloExperimental
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.composeRequestBody
import com.apollographql.apollo3.api.parse
import com.apollographql.apollo3.ApolloRequest
import com.apollographql.apollo3.interceptor.ApolloResponse
import com.apollographql.apollo3.network.HttpExecutionContext
import com.apollographql.apollo3.network.HttpMethod
import com.apollographql.apollo3.network.NetworkTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
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
      serverUrl = HttpUrl.parse(serverUrl)!!,
      headers = Headers.of(headers),
      httpCallFactory = OkHttpClient.Builder()
          .connectTimeout(connectTimeoutMillis, TimeUnit.MILLISECONDS)
          .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
          .build(),
      httpMethod = httpMethod,
  )

  override fun <D : Operation.Data> execute(
      request: ApolloRequest<D>,
      responseAdapterCache: ResponseAdapterCache,
      executionContext: ExecutionContext
  ): Flow<ApolloResponse<D>> {
    return flow {
      val response = suspendCancellableCoroutine<ApolloResponse<D>> { continuation ->
        val httpRequest = request.toHttpRequest(
            executionContext[HttpExecutionContext.Request],
            responseAdapterCache
        )
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
                    runCatching { response.parse(request, responseAdapterCache) }
                        .onSuccess { graphQlResponse -> continuation.resume(graphQlResponse) }
                        .onFailure { e ->
                          response.close()
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

  private fun Headers.toMap(): Map<String, String> {
    return names().map {
      it to get(it)!!
    }.toMap()
  }

  @Suppress("UNCHECKED_CAST")
  private fun <D : Operation.Data> Response.parse(
      request: ApolloRequest<D>,
      responseAdapterCache: ResponseAdapterCache
  ): ApolloResponse<D> {
    if (!isSuccessful) throw ApolloHttpException(
        statusCode = code(),
        headers = headers.toMap(),
        message = "Http request failed with status code `${code()} (${message()})`"
    )

    val responseBody = body() ?: throw ApolloHttpException(
        statusCode = code(),
        headers = headers.toMap(),
        message = "Failed to parse GraphQL http network response: EOF"
    )

    val response = request.operation.parse(
        source = responseBody.source(),
        responseAdapterCache = responseAdapterCache
    )
    return ApolloResponse(
        requestUuid = request.requestUuid,
        response = response,
        executionContext = request.executionContext + HttpExecutionContext.Response(
            statusCode = code(),
            headers = headers.toMap()
        )
    )
  }

  private fun <D : Operation.Data> ApolloRequest<D>.toHttpRequest(
      httpExecutionContext: HttpExecutionContext.Request?,
      responseAdapterCache: ResponseAdapterCache
  ): Request {
    try {
      return when (httpMethod) {
        HttpMethod.Get -> toHttpGetRequest(httpExecutionContext, responseAdapterCache)
        HttpMethod.Post -> toHttpPostRequest(httpExecutionContext, responseAdapterCache)
      }
    } catch (e: Exception) {
      throw ApolloSerializationException(
          message = "Failed to compose GraphQL network request",
          cause = e
      )
    }
  }

  private fun <D : Operation.Data> ApolloRequest<D>.toHttpGetRequest(
      httpExecutionContext: HttpExecutionContext.Request?,
      responseAdapterCache: ResponseAdapterCache
  ): Request {
    val url = serverUrl.newBuilder()
        .addQueryParameter("query", operation.queryDocument())
        .addQueryParameter("operationName", operation.name())
        .apply {
          operation.variables().marshal(responseAdapterCache).let { variables ->
            if (variables.isNotEmpty()) addQueryParameter("variables", variables)
          }
        }
        .build()
    return Request.Builder()
        .url(url)
        .headers(headers)
        .apply {
          httpExecutionContext?.headers?.forEach { (name, value) ->
            header(name, value)
          }
        }
        .build()
  }

  private fun <D : Operation.Data> ApolloRequest<D>.toHttpPostRequest(
      httpExecutionContext: HttpExecutionContext.Request?,
      responseAdapterCache: ResponseAdapterCache
  ): Request {
    val requestBody = operation.composeRequestBody(responseAdapterCache)
        .let {
          RequestBody.create(
              MediaType.parse(MEDIA_TYPE),
              it
          )
        }

    return Request.Builder()
        .url(serverUrl)
        .headers(headers)
        .apply {
          httpExecutionContext?.headers?.forEach { (name, value) ->
            header(name, value)
          }
        }
        .post(requestBody)
        .build()
  }
}
