package com.apollographql.apollo3.tooling

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.compiler.APOLLO_VERSION
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.tooling.platformapi.public.PublishMonolithSchemaMutation
import com.apollographql.apollo3.tooling.platformapi.public.PublishSubgraphSchemaMutation
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
        .httpExposeErrorBody(true)
        .build()

    val graphID = graph ?: key.getGraph() ?: error("graph not found")

    val allHeaders: Map<String, String> = mapOf(
        "x-api-key" to key,
        "apollographql-client-name" to "apollo-tooling",
        "apollographql-client-version" to APOLLO_VERSION
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
    val response = try {
      runBlocking { call.execute() }
    } catch (e: ApolloHttpException) {
      val body = e.body?.use { it.readUtf8() } ?: ""
      throw Exception("Cannot upload schema: (code: ${e.statusCode})\n$body", e)
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
    val response = try {
      runBlocking { call.execute() }
    } catch (e: ApolloHttpException) {
      val body = e.body?.use { it.readUtf8() } ?: ""
      throw Exception("Cannot upload schema: (code: ${e.statusCode})\n$body", e)
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
