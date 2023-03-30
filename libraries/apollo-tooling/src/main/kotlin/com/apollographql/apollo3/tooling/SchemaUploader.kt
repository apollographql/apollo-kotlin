package com.apollographql.apollo3.tooling

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.tooling.platformapi.PublishMonolithSchemaMutation
import com.apollographql.apollo3.tooling.platformapi.PublishSubgraphSchemaMutation
import kotlinx.coroutines.runBlocking

@ApolloExperimental
object SchemaUploader {
  fun uploadSchema(
      sdl: String,
      key: String,
      graph: String?,
      variant: String = "current",
      subgraph: String? = null,
      revision: String? = null,
      headers: Map<String, String> = emptyMap(),
  ) {
    check(subgraph == null && revision == null || subgraph != null && revision != null) {
      "subgraph and revision must be both null or both not null"
    }
    val apolloClient = ApolloClient.Builder()
        .serverUrl("https://api.apollographql.com/graphql")
        .build()

    val graphID = graph ?: key.getGraph() ?: error("graph not found")

    if (subgraph == null) {
      publishMonolithSchema(apolloClient, graphID, variant, sdl, headers, key)
    } else {
      publishSubgraphSchema(apolloClient, graphID, variant, sdl, subgraph, revision, headers, key)
    }
  }

  private fun publishMonolithSchema(
      apolloClient: ApolloClient,
      graphID: String,
      variant: String,
      sdl: String,
      headers: Map<String, String>,
      key: String,
  ) {
    val response = runBlocking {
      apolloClient.mutation(
          PublishMonolithSchemaMutation(
              graphID = graphID,
              variant = variant,
              schemaDocument = sdl,
          )
      )
          .httpHeaders(headers.map { HttpHeader(it.key, it.value) })
          .addHttpHeader("x-api-key", key)
          .execute()
    }
    check(!response.hasErrors()) {
      "Cannot upload schema: ${response.errors!!.joinToString { it.message }}"
    }
    val code = response.data?.graph?.uploadSchema?.code
    val message = response.data?.graph?.uploadSchema?.message
    val success = response.data?.graph?.uploadSchema?.success
    check(success == true) {
      "Cannot upload schema (code: $code): $message"
    }
  }

  private fun publishSubgraphSchema(
      apolloClient: ApolloClient,
      graphID: String,
      variant: String,
      sdl: String,
      subgraph: String,
      revision: String?,
      headers: Map<String, String>,
      key: String,
  ) {
    val response = runBlocking {
      apolloClient.mutation(
          PublishSubgraphSchemaMutation(
              graphID = graphID,
              variant = variant,
              schemaDocument = sdl,
              subgraph = subgraph,
              revision = revision!!,
          )
      )
          .httpHeaders(headers.map { HttpHeader(it.key, it.value) })
          .addHttpHeader("x-api-key", key)
          .execute()
    }
    check(!response.hasErrors()) {
      "Cannot upload schema: ${response.errors!!.joinToString { it.message }}"
    }
    val errors = response.data?.graph?.publishSubgraph?.errors?.filterNotNull()?.joinToString("\n") { it.code + ": " + it.message }
    check(errors.isNullOrEmpty()) {
      "Cannot upload schema:\n$errors"
    }
  }
}
