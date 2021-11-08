package com.apollographql.apollo3.network

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.apollographql.apollo3.network.http.OkHttpEngine
import com.apollographql.apollo3.network.ws.OkHttpWebSocketEngine
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import okhttp3.Call
import okhttp3.OkHttpClient

/**
 * Configures the [ApolloClient] to use the [OkHttpEngine] for network requests.
 */
fun ApolloClient.Builder.okHttpClient(okHttpClient: OkHttpClient) = apply {
  httpEngine(OkHttpEngine(okHttpClient))
  webSocketEngine(OkHttpWebSocketEngine(okHttpClient))
}

/**
 * Configures the [ApolloClient] to use the [callFactory] for network requests.
 */
fun ApolloClient.Builder.okHttpCallFactory(callFactory: Call.Factory) = apply {
  httpEngine(OkHttpEngine(callFactory))
}

/**
 * Configures the [HttpNetworkTransport] to use the [OkHttpEngine] for network requests.
 */
fun HttpNetworkTransport.Builder.okHttpClient(okHttpClient: OkHttpClient) = apply {
  httpEngine(OkHttpEngine(okHttpClient))
}

/**
 * Configures the [HttpNetworkTransport] to use the [okHttpCallFactory] for network requests.
 */
fun HttpNetworkTransport.Builder.okHttpCallFactory(okHttpCallFactory: Call.Factory) = apply {
  httpEngine(OkHttpEngine(okHttpCallFactory))
}

/**
 * Configures the [WebSocketNetworkTransport] to use the [okHttpCallFactory] for network requests.
 */
fun WebSocketNetworkTransport.Builder.okHttpClient(okHttpClient: OkHttpClient) = apply {
  webSocketEngine(OkHttpWebSocketEngine(okHttpClient))
}
