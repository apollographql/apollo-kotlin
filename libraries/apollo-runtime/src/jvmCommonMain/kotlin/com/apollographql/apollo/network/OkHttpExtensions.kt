@file:Suppress("DEPRECATION")

package com.apollographql.apollo.network

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.annotations.ApolloDeprecatedSince
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
@Deprecated(
    "Use networkTransport() instead",
    ReplaceWith(
        "networkTransport(HttpNetworkTransport.Builder().httpEngine(DefaultHttpEngine(okHttpClient)).build())" +
            ".subscriptionNetworkTransport(WebSocketNetworkTransport.Builder().webSocketEngine(DefaultWebSocketEngine(okHttpClient)).build())",
        "com.apollographql.apollo.network.http.HttpNetworkTransport",
        "com.apollographql.apollo.network.http.DefaultHttpEngine",
        "com.apollographql.apollo.network.ws.DefaultWebSocketEngine",
        "com.apollographql.apollo.network.ws.WebSocketNetworkTransport"
    )
)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_1_1)
fun ApolloClient.Builder.okHttpClient(okHttpClient: OkHttpClient) = apply {
  httpEngine(DefaultHttpEngine(okHttpClient))
  webSocketEngine(DefaultWebSocketEngine(okHttpClient))
}

/**
 * Configures the [ApolloClient] to use the [callFactory] for network requests.
 */
@Deprecated(
    "Use networkTransport() instead",
    ReplaceWith(
        "networkTransport(HttpNetworkTransport.Builder().httpEngine(DefaultHttpEngine(callFactory)).build())",
        "com.apollographql.apollo.network.http.HttpNetworkTransport",
        "com.apollographql.apollo.network.http.DefaultHttpEngine",
    )
)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_1_1)
fun ApolloClient.Builder.okHttpCallFactory(callFactory: Call.Factory) = apply {
  httpEngine(DefaultHttpEngine(callFactory))
}

/**
 * Configures the [ApolloClient] to use the lazily initialized [callFactory] for network requests.
 */
@Deprecated(
    "Use networkTransport() instead",
    ReplaceWith(
        "networkTransport(HttpNetworkTransport.Builder().httpEngine(DefaultHttpEngine(callFactory)).build())",
        "com.apollographql.apollo.network.http.HttpNetworkTransport",
        "com.apollographql.apollo.network.http.DefaultHttpEngine",
    )
)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_1_1)
fun ApolloClient.Builder.okHttpCallFactory(callFactory: () -> Call.Factory) = apply {
  httpEngine(DefaultHttpEngine(callFactory))
}

/**
 * Configures the [HttpNetworkTransport] to use the [DefaultHttpEngine] for network requests.
 */
@Deprecated("Use httpEngine instead.", ReplaceWith("httpEngine(DefaultHttpEngine(okHttpClient))"))
fun HttpNetworkTransport.Builder.okHttpClient(okHttpClient: OkHttpClient) = apply {
  httpEngine(DefaultHttpEngine(okHttpClient))
}

/**
 * Configures the [OkHttpClient] to use for HTTP requests.
 *
 * This is the same function as [okHttpCallFactory]
 */
@Deprecated("Use webSocketEngine instead.", ReplaceWith("webSocketEngine(DefaultWebSocketEngine(okHttpClient))"))
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_1_1)
fun WebSocketNetworkTransport.Builder.okHttpClient(okHttpClient: OkHttpClient) = apply {
  webSocketEngine(DefaultWebSocketEngine(okHttpClient))
}

/**
 * Configures the [Call.Factory] to use for HTTP requests.
 */
@Deprecated("Use httpEngine instead.", ReplaceWith("httpEngine(DefaultHttpEngine(callFactory))"))
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_1_1)
fun HttpNetworkTransport.Builder.okHttpCallFactory(callFactory: Call.Factory) = apply {
  httpEngine(DefaultHttpEngine(callFactory))
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