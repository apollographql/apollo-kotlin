package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BearerTokenInterceptor(private val tokenProvider: TokenProvider) : HttpInterceptor {
  private val mutex = Mutex()

  override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
    var token = mutex.withLock { tokenProvider.currentToken() }

    val response = chain.proceed(request.newBuilder().addHeader("Authorization", "Bearer $token").build())

    return if (response.statusCode == 401) {
      token = mutex.withLock { tokenProvider.refreshToken(token) }
      chain.proceed(request.newBuilder().addHeader("Authorization", "Bearer $token").build())
    } else {
      response
    }
  }
}
