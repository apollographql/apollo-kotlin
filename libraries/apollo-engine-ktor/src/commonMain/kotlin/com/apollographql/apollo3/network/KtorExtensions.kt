@file:Suppress("DEPRECATION")

package com.apollographql.apollo3.network

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.network.http.KtorHttpEngine
import com.apollographql.apollo3.network.ws.KtorWebSocketEngine
import io.ktor.client.HttpClient

/**
 * Configures the [ApolloClient] to use the Ktor [HttpClient] for network requests.
 * The [HttpClient] will be used for both HTTP and WebSocket requests.
 *
 * See also [ApolloClient.Builder.httpEngine] and [ApolloClient.Builder.webSocketEngine]
 */
@ApolloExperimental
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@Deprecated("apollo-engine-ktor has moved to 'com.apollographql.ktor:apollo-engine-ktor'")
fun ApolloClient.Builder.ktorClient(httpClient: HttpClient) = apply {
  httpEngine(KtorHttpEngine(httpClient))
  webSocketEngine(KtorWebSocketEngine(httpClient))
}
