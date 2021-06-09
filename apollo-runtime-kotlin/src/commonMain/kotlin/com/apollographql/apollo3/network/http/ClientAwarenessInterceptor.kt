package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse

/**
 * A [HttpInterceptor] to add [Client Awareness](https://www.apollographql.com/docs/studio/client-awareness/).
 */
class ApolloClientAwarenessInterceptor(clientName: String, clientVersion: String) : HttpInterceptor {
  private val extraHeaders = mapOf(
      "apollographql-client-name" to clientName,
      "apollographql-client-version" to clientVersion
  )

  override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
     return chain.proceed(request.copy(headers = request.headers + extraHeaders))
  }
}

