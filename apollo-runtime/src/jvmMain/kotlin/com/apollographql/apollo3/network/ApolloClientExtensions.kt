package com.apollographql.apollo3.network

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.http.OkHttpEngine
import com.apollographql.apollo3.network.ws.OkHttpWebSocketEngine
import okhttp3.Call
import okhttp3.OkHttpClient

/**
 * Configures the [ApolloClient] to use the [OkHttpEngine] for network requests.
 */
fun ApolloClient.Builder.okHttpClient(okHttpClient: OkHttpClient) {
  httpEngine(OkHttpEngine(okHttpClient))
  webSocketEngine(OkHttpWebSocketEngine(okHttpClient))
}

/**
 * Configures the [ApolloClient] to use the [callFactory] for network requests.
 */
fun ApolloClient.Builder.okHttpCallFactory(callFactory: Call.Factory) {
  httpEngine(OkHttpEngine(callFactory))
}