package com.apollographql.apollo3.tooling

import com.apollographql.apollo3.ApolloClient

internal const val INTERNAL_PLATFORM_API_URL = "https://graphql.api.apollographql.com/api/graphql"

internal fun newInternalPlatformApiApolloClient(
    apiKey: String,
    serverUrl: String = INTERNAL_PLATFORM_API_URL,
): ApolloClient {
  val apolloClient = ApolloClient.Builder()
      .serverUrl(serverUrl)
      .httpExposeErrorBody(true)
      .addHttpHeader("x-api-key", apiKey)
      .build()
  return apolloClient
}
