package com.apollographql.apollo3.testing

import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.network.http.HttpEngine
import com.apollographql.apollo3.network.http.HttpRequest
import com.apollographql.apollo3.network.http.HttpResponse
import kotlinx.coroutines.channels.Channel
import okio.Buffer

class TestHttpEngine : HttpEngine {
  private val channel = Channel<HttpResponse> (capacity = Channel.UNLIMITED)

  fun offer(httpResponse: HttpResponse) {
    channel.offer(httpResponse)
  }

  fun offer(string: String) {
    channel.offer(HttpResponse(
        statusCode = 200,
        headers = emptyMap(),
        body = Buffer().writeUtf8(string)
    ))
  }

  override suspend fun <R> execute(request: HttpRequest, block: (HttpResponse) -> R): R {
    if (channel.isEmpty) {
      throw ApolloNetworkException(message = "No Response")
    }

    return block(channel.receive())
  }
}