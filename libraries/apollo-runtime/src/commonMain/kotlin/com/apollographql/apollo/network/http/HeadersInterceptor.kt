package com.apollographql.apollo.network.http

import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse

class HeadersInterceptor(private val headers: List<HttpHeader>) : HttpInterceptor {
  override suspend fun intercept(
      request: HttpRequest,
      chain: HttpInterceptorChain,
  ): HttpResponse {
    return chain.proceed(request.newBuilder().addHeaders(headers).build())
  }
}
