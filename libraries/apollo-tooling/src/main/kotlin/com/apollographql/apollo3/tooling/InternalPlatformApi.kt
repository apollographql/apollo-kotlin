package com.apollographql.apollo.tooling

import com.apollographql.apollo.ApolloClient

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
      .build()
  return apolloClient
}
