package com.apollographql.apollo

import okhttp3.Interceptor
import okhttp3.Response

/**
 * An [Interceptor] to add [Client Awareness](https://www.apollographql.com/docs/studio/client-awareness/).
 *
 * Add this interceptor to your [okhttp3.OkHttpClient] instance when creating your [ApolloClient]
 */
class ApolloClientAwarenessInterceptor(val clientName: String, val clientVersion: String): Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val newRequest = chain.request().newBuilder()
        .addHeader("apollographql-client-name", clientName)
        .addHeader("apollographql-client-version", clientVersion)
        .build()

    return chain.proceed(newRequest)
  }
}
