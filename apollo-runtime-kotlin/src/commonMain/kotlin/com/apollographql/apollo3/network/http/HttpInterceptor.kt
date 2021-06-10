package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse

interface HttpInterceptorChain {
  suspend fun proceed(request: HttpRequest): HttpResponse
}

interface HttpInterceptor {
  suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse
}

internal class RealInterceptorChain(
    private val interceptors: List<HttpInterceptor>,
    private val index: Int,
) : HttpInterceptorChain {

  override suspend fun proceed(request: HttpRequest): HttpResponse {
    check(index < interceptors.size)
    return interceptors[index].intercept(
        request,
        RealInterceptorChain(
            interceptors = interceptors,
            index = index + 1,
        )
    )
  }
}
