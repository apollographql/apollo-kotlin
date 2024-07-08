package com.apollographql.apollo.tooling

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.compiler.APOLLO_VERSION

internal const val graphosEndpoint = "https://api.apollographql.com/graphql"

internal val apolloClient =  ApolloClient.Builder()
    .serverUrl(graphosEndpoint)
    .httpExposeErrorBody(true)
    .addHttpHeader("apollographql-client-name", "apollo-tooling")
    .addHttpHeader("apollographql-client-version", APOLLO_VERSION)
    .build()

/**
 * @return the graph from a service key like "service:$graph:$token"
 *
 * This will not work with user keys
 */
internal fun String.getGraph(): String? {
  if (!startsWith("service:")) {
    return null
  }
  return split(":")[1]
}
