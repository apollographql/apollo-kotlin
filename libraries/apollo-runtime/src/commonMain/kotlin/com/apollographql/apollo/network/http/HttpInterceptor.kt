package com.apollographql.apollo.network.http

import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.exception.ApolloNetworkException

interface HttpInterceptorChain {
  /**
   * @throws [ApolloNetworkException] if a network error happens
   */
  suspend fun proceed(request: HttpRequest): HttpResponse
}

interface HttpInterceptor {
  suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse
  fun dispose() {}
}

internal class DefaultHttpInterceptorChain(
    private val interceptors: List<HttpInterceptor>,
    private val index: Int,
) : HttpInterceptorChain {

  override suspend fun proceed(request: HttpRequest): HttpResponse {
    check(index < interceptors.size)
    return interceptors[index].intercept(
        request,
        DefaultHttpInterceptorChain(
            interceptors = interceptors,
            index = index + 1,
        )
    )
  }
}
