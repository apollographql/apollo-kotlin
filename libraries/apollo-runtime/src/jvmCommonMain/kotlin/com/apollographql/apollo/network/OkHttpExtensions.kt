@file:Suppress("DEPRECATION")

package com.apollographql.apollo.network

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.network.http.DefaultHttpEngine
import com.apollographql.apollo.network.http.HttpNetworkTransport
import com.apollographql.apollo.network.websocket.WebSocketEngine
import com.apollographql.apollo.network.websocket.WebSocketNetworkTransport
import com.apollographql.apollo.network.ws.DefaultWebSocketEngine
import com.apollographql.apollo.network.ws.WebSocketNetworkTransport as DeprecatedWebSocketNetworkTransport
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
  subscriptionNetworkTransport(
      WebSocketNetworkTransport.Builder()
          .webSocketEngine(WebSocketEngine(okHttpClient))
          .build()
  )
}

/**
 * Configures the [ApolloClient] to use the [callFactory] for network requests.
 */
fun ApolloClient.Builder.okHttpCallFactory(callFactory: Call.Factory) = apply {
  @Suppress("DEPRECATION")
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
  @Suppress("DEPRECATION")
  httpEngine(DefaultHttpEngine(okHttpClient))
}

/**
 * Configures the [HttpNetworkTransport] to use the [okHttpCallFactory] for network requests.
 */
fun HttpNetworkTransport.Builder.okHttpCallFactory(okHttpCallFactory: Call.Factory) = apply {
  @Suppress("DEPRECATION")
  httpEngine(DefaultHttpEngine(okHttpCallFactory))
}

/**
 * Configures the [DeprecatedWebSocketNetworkTransport] to use the [okHttpCallFactory] for network requests.
 */
@Deprecated("The websocket implementation has moved to 'com.apollographql.apollo.network.websocket'. See https://go.apollo.dev/ak-v5-websockets for more details.")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
fun DeprecatedWebSocketNetworkTransport.Builder.okHttpClient(okHttpClient: OkHttpClient) = apply {
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