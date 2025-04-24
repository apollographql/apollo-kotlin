package com.apollographql.apollo.tooling

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.network.NetworkTransport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal const val INTERNAL_PLATFORM_API_URL = "https://graphql.api.apollographql.com/api/graphql"

internal fun newInternalPlatformApiApolloClient(
    apiKey: String? = null,
    serverUrl: String = INTERNAL_PLATFORM_API_URL,
): ApolloClient {
  val apolloClient = ApolloClient.Builder()
      .serverUrl(serverUrl)
      .httpExposeErrorBody(true)
      .apply {
        if (apiKey != null) addHttpHeader("x-api-key", apiKey)
      }
      // Use a dummy NetworkTransport for subscriptions (which are not used), because this is used by the IDE plugin
      // and the default NetworkTransport references CoroutineDispatcher.limitedParallelism which doesn't exist in certain
      // IDE versions. See https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#coroutinesLibraries.
      .subscriptionNetworkTransport(object : NetworkTransport {
        override fun <D : Operation.Data> execute(request: ApolloRequest<D>): Flow<ApolloResponse<D>> = emptyFlow()
        override fun dispose() {}
      })
      .build()
  return apolloClient
}
