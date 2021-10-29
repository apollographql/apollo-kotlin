package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse

class HeadersInterceptor(private val headers: List<HttpHeader>) : HttpInterceptor {
  override suspend fun intercept(
      request: HttpRequest,
      chain: HttpInterceptorChain,
  ): HttpResponse {
    return chain.proceed(request.newBuilder().addHeaders(headers).build())
  }
}
