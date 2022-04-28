@file:Suppress("DEPRECATION")

package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Deprecated("BearerTokenInterceptor was provided as an example but is too simple for most use cases." +
    "Define your own interceptor or take a look at https://www.apollographql.com/docs/kotlin/advanced/interceptors-http" +
    " for more details.")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_2_3)
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
