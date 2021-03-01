package com.apollographql.apollo3.testing

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.toResponse
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.network.http.HttpEngine
import com.apollographql.apollo3.network.http.HttpRequest
import com.apollographql.apollo3.network.http.HttpResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import okio.Buffer

class TestHttpEngine : HttpEngine {
  fun interface ResponseProvider {
    fun response(request: HttpRequest): HttpResponse
  }
  private val channel = Channel<ResponseProvider>(capacity = Channel.UNLIMITED)

  fun enqueue(responseProvider: (HttpRequest) -> HttpResponse) {
    channel.offer(responseProvider)
  }

  fun enqueue(httpResponse: HttpResponse) {
    enqueue {
      httpResponse
    }
  }

  fun <D : Operation.Data> enqueue(
      operation: Operation<D>,
      data: D,
      responseAdapterCache: ResponseAdapterCache = ResponseAdapterCache.DEFAULT
  ) {
    val json = operation.toResponse(data, responseAdapterCache = responseAdapterCache)
    enqueue(json)
  }

  fun enqueue(string: String) {
    enqueue(HttpResponse(
        statusCode = 200,
        headers = emptyMap(),
        body = Buffer().writeUtf8(string)
    ))
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override suspend fun <R> execute(request: HttpRequest, block: (HttpResponse) -> R): R {
    if (channel.isEmpty) {
      throw ApolloNetworkException(message = "No Response")
    }

    return block(channel.receive().response(request))
  }
}