package com.apollographql.apollo.network

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.network.http.DefaultHttpEngine
import com.apollographql.apollo.network.http.HttpNetworkTransport
import com.apollographql.apollo.network.ws.DefaultWebSocketEngine
import com.apollographql.apollo.network.ws.WebSocketNetworkTransport
import okhttp3.Call
import okhttp3.Headers
import okhttp3.OkHttpClient

/**
 * Configures the [ApolloClient] to use the [OkHttpClient] for network requests.
 * The [OkHttpClient] will be used for both HTTP and WebSocket requests.
 *
 * See also [ApolloClient.Builder.httpEngine] and [ApolloClient.Builder.webSocketEngine]
 */
fun ApolloClient.Builder.okHttpClient(okHttpClient: OkHttpClient) = apply {
  httpEngine(DefaultHttpEngine(okHttpClient))
  webSocketEngine(DefaultWebSocketEngine(okHttpClient))
}

/**
 * Configures the [ApolloClient] to use the [callFactory] for network requests.
 */
fun ApolloClient.Builder.okHttpCallFactory(callFactory: Call.Factory) = apply {
  httpEngine(DefaultHttpEngine(callFactory))
}

/**
 * Configures the [ApolloClient] to use the lazily initialized [callFactory] for network requests.
 */
fun ApolloClient.Builder.okHttpCallFactory(callFactory: () -> Call.Factory) = apply {
  httpEngine(DefaultHttpEngine(callFactory))
}

/**
 * Configures the [HttpNetworkTransport] to use the [DefaultHttpEngine] for network requests.
 */
fun HttpNetworkTransport.Builder.okHttpClient(okHttpClient: OkHttpClient) = apply {
  httpEngine(DefaultHttpEngine(okHttpClient))
}

/**
 * Configures the [HttpNetworkTransport] to use the [okHttpCallFactory] for network requests.
 */
fun HttpNetworkTransport.Builder.okHttpCallFactory(okHttpCallFactory: Call.Factory) = apply {
  httpEngine(DefaultHttpEngine(okHttpCallFactory))
}

/**
 * Configures the [WebSocketNetworkTransport] to use the [okHttpCallFactory] for network requests.
 */
fun WebSocketNetworkTransport.Builder.okHttpClient(okHttpClient: OkHttpClient) = apply {
  webSocketEngine(DefaultWebSocketEngine(okHttpClient))
}

internal fun List<HttpHeader>.toOkHttpHeaders(): Headers =
    Headers.Builder().also { headers ->
      this.forEach {
        headers.add(it.name, it.value)
      }
    }.build()

internal val defaultOkHttpClientBuilder: OkHttpClient.Builder by lazy {
  OkHttpClient.Builder()
}