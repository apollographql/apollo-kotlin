package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse

interface HttpInterceptorChain {
  suspend fun <R> proceed(request: HttpRequest, block: (HttpResponse) -> R): R
}

interface HttpRequestInterceptor {
  suspend fun <R> intercept(request: HttpRequest, block: (HttpResponse) -> R, chain: HttpInterceptorChain): R
}

internal class RealInterceptorChain(
    private val interceptors: List<HttpRequestInterceptor>,
    private val index: Int,
) : HttpInterceptorChain {

  override suspend fun <R> proceed(request: HttpRequest, block: (HttpResponse) -> R): R {
    check(index < interceptors.size)
    return interceptors[index].intercept(
        request,
        block,
        RealInterceptorChain(
            interceptors = interceptors,
            index = index + 1,
        )
    )
  }
}
