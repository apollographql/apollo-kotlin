package com.apollographql.apollo.network

import com.apollographql.apollo.ApolloError
import com.apollographql.apollo.ApolloException
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.internal.json.JsonWriter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
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
import okio.Buffer
import java.io.IOException

private const val MEDIA_TYPE = "application/json; charset=utf-8"

@ApolloExperimental
@ExperimentalCoroutinesApi
actual class ApolloHttpNetworkTransport(
    private val serverUrl: HttpUrl,
    private val httpHeaders: Headers,
    private val httpCallFactory: Call.Factory,
    private val httpMethod: HttpMethod
) : NetworkTransport {

  actual constructor(
      serverUrl: String,
      httpHeaders: Map<String, String>,
      httpMethod: HttpMethod
  ) : this(
      serverUrl = serverUrl.toHttpUrl(),
      httpHeaders = httpHeaders.toHeaders(),
      httpCallFactory = OkHttpClient(),
      httpMethod = httpMethod
  )

  override fun execute(request: GraphQLRequest): Flow<GraphQLResponse> {
    return callbackFlow {
      val httpRequest = request.toHttpRequest()
      val call = httpCallFactory.newCall(httpRequest).apply {
        enqueue(
            object : Callback {
              override fun onFailure(call: Call, e: IOException) {
                if (!isActive) return
                val apolloException = ApolloException(
                    message = "Failed to execute GraphQL http network request",
                    error = ApolloError.Network,
                    cause = e
                )
                cancel(message = apolloException.message, cause = apolloException)
              }

              override fun onResponse(call: Call, response: Response) {
                if (!isActive) return
                runCatching { response.parse() }
                    .onSuccess { graphQlResponse ->
                      runCatching {
                        offer(graphQlResponse)
                        close()
                      }
                    }
                    .onFailure { e ->
                      if (e is ApolloException) {
                        cancel(message = e.message, cause = e)
                      } else {
                        val apolloException = ApolloException(
                            message = "Failed to parse GraphQL http network response",
                            error = ApolloError.ParseError
                        )
                        cancel(message = apolloException.message, cause = apolloException)
                      }
                    }
              }
            }
        )
      }
      awaitClose {
        call.cancel()
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun Response.parse(): GraphQLResponse {
    if (!isSuccessful) throw ApolloException(
        message = "Http request failed with status code `$code ($message)`",
        error = ApolloError.Network
    )

    val responseBody = body ?: throw ApolloException(
        message = "Failed to parse GraphQL http network response: EOF",
        error = ApolloError.Network
    )

    return GraphQLResponse(
        body = responseBody.source(),
        executionContext = ExecutionContext.Empty
    )
  }

  private fun GraphQLRequest.toHttpRequest(): Request {
    return when (httpMethod) {
      HttpMethod.Get -> toHttpGetRequest()
      HttpMethod.Post -> toHttpPostRequest()
    }
  }

  private fun GraphQLRequest.toHttpGetRequest(): Request {
    val url = serverUrl.newBuilder()
        .addQueryParameter("query", document)
        .addQueryParameter("operationName", operationName)
        .apply { if (variables.isNotEmpty()) addQueryParameter("variables", variables) }
        .build()
    return Request.Builder()
        .url(url)
        .headers(httpHeaders)
        .build()
  }

  private fun GraphQLRequest.toHttpPostRequest(): Request {
    val buffer = Buffer()
    JsonWriter.of(buffer)
        .name("operationName").value(operationName)
        .name("query").value(document)
        .name("variables").value(variables)
        .close()
    val requestBody = buffer.readByteArray().toRequestBody(contentType = MEDIA_TYPE.toMediaType())
    return Request.Builder()
        .url(serverUrl)
        .headers(httpHeaders)
        .post(requestBody)
        .build()
  }
}
