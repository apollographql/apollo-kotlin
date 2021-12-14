package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse

/**
 * A [HttpInterceptor] to add [Client Awareness](https://www.apollographql.com/docs/studio/client-awareness/).
 */
class ApolloClientAwarenessInterceptor(clientName: String, clientVersion: String) : HttpInterceptor {
  private val extraHeaders = listOf(
      HttpHeader("apollographql-client-name", clientName),
      HttpHeader("apollographql-client-version", clientVersion)
  )

  override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
     return chain.proceed(request.newBuilder().addHeaders(extraHeaders).build())
  }
}

