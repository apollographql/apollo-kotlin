package com.apollographql.apollo.tooling

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.tooling.platformapi.public.PublishMonolithSchemaMutation
import com.apollographql.apollo.tooling.platformapi.public.PublishSubgraphSchemaMutation
import kotlinx.coroutines.runBlocking

@ApolloExperimental
object SchemaUploader {
  fun uploadSchema(
      sdl: String,
      apolloKey: String,
      graph: String?,
      variant: String = "current",
      subgraph: String? = null,
      revision: String? = null,
      headers: Map<String, String> = emptyMap(),
  ) {
    check(subgraph == null && revision == null || subgraph != null && revision != null) {
      "subgraph and revision must be both null or both not null"
    }
    val graphID = graph ?: apolloKey.getGraph() ?: error("graph not found")

    val allHeaders: Map<String, String> = mapOf(
        "x-api-key" to apolloKey,
    ) + headers

    if (subgraph == null) {
      publishMonolithSchema(
          apolloClient = apolloClient,
          graphID = graphID,
          variant = variant,
          sdl = sdl,
          headers = allHeaders
      )
    } else {
      publishSubgraphSchema(
          apolloClient = apolloClient,
          graphID = graphID,
          variant = variant,
          sdl = sdl,
          subgraph = subgraph,
          revision = revision,
          headers = allHeaders
      )
    }
  }

  private fun publishMonolithSchema(
      apolloClient: ApolloClient,
      graphID: String,
      variant: String,
      sdl: String,
      headers: Map<String, String>,
  ) {
    val call = apolloClient.mutation(
        PublishMonolithSchemaMutation(
            graphID = graphID,
            variant = variant,
            schemaDocument = sdl,
        )
    )
        .httpHeaders(headers.map { HttpHeader(it.key, it.value) })
    val response = runBlocking { call.execute() }
    val data = response.data
    if (data == null) {
      throw response.toException("Cannot upload schema")
    }

    if (data.graph == null) {
      throw Exception("Cannot retrieve graph '$graphID': ${response.errors?.joinToString { it.message }}")
    }

    val code = data.graph.uploadSchema.code
    val message = data.graph.uploadSchema.message
    val success = data.graph.uploadSchema.success
    if (!success) {
      throw Exception("Cannot upload schema (code: $code): $message")
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
  ) {
    val call = apolloClient.mutation(
        PublishSubgraphSchemaMutation(
            graphID = graphID,
            variant = variant,
            schemaDocument = sdl,
            subgraph = subgraph,
            revision = revision!!,
        )
    )
        .httpHeaders(headers.map { HttpHeader(it.key, it.value) })
    val response = runBlocking { call.execute() }
    val data = response.data
    if (data == null) {
      throw response.toException("Cannot upload schema")
    }
    if (data.graph == null) {
      throw Exception("Cannot find graph '$graphID': ${response.errors?.joinToString { it.message }}")
    }
    val errors = data.graph.publishSubgraph.errors.filterNotNull().joinToString("\n") { it.code + ": " + it.message }
    if (errors.isNotEmpty()) {
      throw Exception("Cannot upload schema: $errors")
    }
  }
}
