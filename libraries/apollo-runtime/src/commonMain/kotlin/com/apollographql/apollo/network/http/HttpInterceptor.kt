package com.apollographql.apollo.network.http

import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloNetworkException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException

interface HttpInterceptorChain {
  /**
   * Continues with the request and call all downstream interceptors.
   */
  @Throws(Throwable::class)
  suspend fun proceed(request: HttpRequest): HttpResponse
}

interface HttpInterceptor {
  /**
   * Intercepts the request and returns a response.
   * Implementation may throw in case of error. Those errors are typically
   * caught by the network transport and subsequently exposed in `ApolloResponse.exception`.
   * If the exception is not an instance of [ApolloException], it will be wrapped in an
   * instance of [ApolloNetworkException].
   */
  @Throws(Throwable::class)
  suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse

  // TODO: remove dispose
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
