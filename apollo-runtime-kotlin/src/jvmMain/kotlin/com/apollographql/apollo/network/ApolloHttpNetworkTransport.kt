package com.apollographql.apollo.network

import com.apollographql.apollo.ApolloException
import com.apollographql.apollo.ApolloHttpException
import com.apollographql.apollo.ApolloNetworkException
import com.apollographql.apollo.ApolloParseException
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.internal.json.JsonWriter
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
import okio.Buffer
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val MEDIA_TYPE = "application/json; charset=utf-8"

@ApolloExperimental
@ExperimentalCoroutinesApi
actual class ApolloHttpNetworkTransport(
    private val serverUrl: HttpUrl,
    private val headers: Headers,
    private val httpCallFactory: Call.Factory,
    private val httpMethod: HttpMethod
) : NetworkTransport {

  actual constructor(
      serverUrl: String,
      headers: Map<String, String>,
      httpMethod: HttpMethod
  ) : this(
      serverUrl = serverUrl.toHttpUrl(),
      headers = headers.toHeaders(),
      httpCallFactory = OkHttpClient(),
      httpMethod = httpMethod
  )

  override fun execute(request: GraphQLRequest, executionContext: ExecutionContext): Flow<GraphQLResponse> {
    return flow {
      val response = suspendCancellableCoroutine<GraphQLResponse> { continuation ->
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
  private fun Response.parse(request: GraphQLRequest): GraphQLResponse {
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

    return GraphQLResponse(
        body = responseBody.source(),
        executionContext = HttpExecutionContext.Response(
            statusCode = code,
            headers = headers.toMap()
        ),
        requestUuid = request.uuid
    )
  }

  private fun GraphQLRequest.toHttpRequest(httpExecutionContext: HttpExecutionContext.Request?): Request {
    return when (httpMethod) {
      HttpMethod.Get -> toHttpGetRequest(httpExecutionContext)
      HttpMethod.Post -> toHttpPostRequest(httpExecutionContext)
    }
  }

  private fun GraphQLRequest.toHttpGetRequest(httpExecutionContext: HttpExecutionContext.Request?): Request {
    val url = serverUrl.newBuilder()
        .addQueryParameter("query", document)
        .addQueryParameter("operationName", operationName)
        .apply { if (variables.isNotEmpty()) addQueryParameter("variables", variables) }
        .build()
    return Request.Builder()
        .url(url)
        .headers(headers)
        .apply {
          httpExecutionContext?.headers?.forEach { name, value ->
            header(name = name, value = value)
          }
        }
        .build()
  }

  private fun GraphQLRequest.toHttpPostRequest(httpExecutionContext: HttpExecutionContext.Request?): Request {
    val buffer = Buffer()
    JsonWriter.of(buffer)
        .beginObject()
        .name("operationName").value(operationName)
        .name("query").value(document)
        .name("variables").jsonValue(variables)
        .endObject()
        .close()
    val requestBody = buffer.readByteArray().toRequestBody(contentType = MEDIA_TYPE.toMediaType())
    return Request.Builder()
        .url(serverUrl)
        .headers(headers)
        .apply {
          httpExecutionContext?.headers?.forEach { name, value ->
            header(name = name, value = value)
          }
        }
        .post(requestBody)
        .build()
  }
}
