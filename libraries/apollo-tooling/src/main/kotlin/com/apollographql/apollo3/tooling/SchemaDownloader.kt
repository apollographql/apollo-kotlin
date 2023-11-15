package com.apollographql.apollo3.tooling

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.ast.introspection.IntrospectionSchema
import com.apollographql.apollo3.ast.introspection.toGQLDocument
import com.apollographql.apollo3.ast.introspection.toIntrospectionCapabilities
import com.apollographql.apollo3.ast.introspection.toIntrospectionSchema
import com.apollographql.apollo3.ast.introspection.writeTo
import com.apollographql.apollo3.ast.toFullSchemaGQLDocument
import com.apollographql.apollo3.ast.toGQLDocument
import com.apollographql.apollo3.ast.toSDL
import com.apollographql.apollo3.exception.ApolloGraphQLException
import com.apollographql.apollo3.network.okHttpClient
import com.apollographql.apollo3.tooling.platformapi.public.DownloadSchemaQuery
import kotlinx.coroutines.runBlocking
import java.io.File


@ApolloExperimental
object SchemaDownloader {
  /**
   * Main entry point for downloading a schema either from introspection or from the Apollo Studio registry
   *
   * One of [endpoint] (for introspection) or [key] (for registry) is required.
   *
   * @param endpoint url of the GraphQL endpoint for introspection
   * @param graph [Apollo Studio users only] The identifier of the Apollo graph used to download the schema.
   * @param key [Apollo Studio users only] The Apollo API key. See https://www.apollographql.com/docs/studio/api-keys/ for more information on how to get your API key.
   * @param graphVariant [Apollo Studio users only] The variant of the Apollo graph used to download the schema.
   * @param registryUrl [Apollo Studio users only] The registry url of the registry instance used to download the schema.
   * Defaults to "https://graphql.api.apollographql.com/api/graphql"
   * @param schema the file where to store the schema. If the file extension is ".json" it will be stored in introspection format.
   * Else it will use SDL. Prefer SDL if you can as it is more compact and carries more information.
   * @param insecure if set to true, TLS/SSL certificates will not be checked when downloading.
   * @param headers extra HTTP headers to send during introspection.
   */
  fun download(
      endpoint: String?,
      graph: String?,
      key: String?,
      graphVariant: String,
      registryUrl: String = "https://api.apollographql.com/graphql",
      schema: File,
      insecure: Boolean = false,
      headers: Map<String, String> = emptyMap(),
  ) {
    var introspectionDataJson: String? = null
    var introspectionSchema: IntrospectionSchema? = null
    var sdlSchema: String? = null

    when {
      endpoint != null -> {
        introspectionDataJson = downloadIntrospection(
            endpoint = endpoint,
            headers = headers,
            insecure = insecure,
        )
        introspectionSchema = try {
          introspectionDataJson.toIntrospectionSchema()
        } catch (e: Exception) {
          throw Exception("Response from $endpoint could not be parsed as a valid schema. Body:\n$introspectionDataJson", e)
        }
      }

      else -> {
        check(key != null) {
          "Apollo: either endpoint (for introspection) or key (for registry) is required"
        }
        val graph2 = graph ?: key.getGraph()
        check(graph2 != null) {
          "Apollo: graph is required to download from the registry"
        }

        sdlSchema = downloadRegistry(
            graph = graph2,
            key = key,
            variant = graphVariant,
            endpoint = registryUrl,
            headers = headers,
            insecure = insecure,
        )
      }
    }

    schema.parentFile?.mkdirs()

    if (schema.extension.lowercase() == "json") {
      if (introspectionSchema == null) {
        check(sdlSchema != null)
        // Convert from SDL to JSON
        sdlSchema
            .toGQLDocument()
            .toFullSchemaGQLDocument()
            .toIntrospectionSchema()
            .writeTo(schema)
      } else {
        check(introspectionDataJson != null)
        // Copy Json verbatim
        schema.writeText(introspectionDataJson)
      }
    } else {
      if (sdlSchema == null) {
        check(introspectionSchema != null)
        // Convert from JSON to SDL
        schema.writeText(introspectionSchema.toGQLDocument().toSDL(indent = "  "))
      } else {
        // Copy SDL verbatim
        schema.writeText(sdlSchema)
      }
    }
  }

  fun downloadIntrospection(
      endpoint: String,
      headers: Map<String, String>,
      insecure: Boolean,
  ): String {
    val metaIntrospectionDataJson = SchemaHelper.executeMetaIntrospectionQuery(
        endpoint = endpoint,
        headers = headers,
        insecure = insecure,
    )
    val introspectionCapabilities = try {
      metaIntrospectionDataJson.toIntrospectionCapabilities()
    } catch (e: Exception) {
      throw Exception("Response from $endpoint could not be parsed as a valid schema. Body:\n$metaIntrospectionDataJson", e)
    }
    return SchemaHelper.executeSchemaQuery(
        introspectionCapabilities = introspectionCapabilities,
        endpoint = endpoint,
        headers = headers,
        insecure = insecure,
    )
  }

  fun downloadRegistry(
      key: String,
      graph: String,
      variant: String,
      endpoint: String = "https://api.apollographql.com/graphql",
      headers: Map<String, String>,
      insecure: Boolean,
  ): String {
    val apolloClient = ApolloClient.Builder()
        .serverUrl(endpoint)
        .okHttpClient(SchemaHelper.newOkHttpClient(insecure))
        .build()
    val response = runBlocking {
      apolloClient.query(DownloadSchemaQuery(graphID = graph, variant = variant))
          .httpHeaders(headers.map { HttpHeader(it.key, it.value) } + HttpHeader("x-api-key", key))
          .execute()
    }
    response.exception?.let {
      throw if (it is ApolloGraphQLException) {
        Exception("Cannot retrieve document from $endpoint: ${response.errors!!.joinToString { it.message }}\nCheck graph id and variant", it)
      } else {
        it
      }
    }
    val document = response.data?.graph?.variant?.latestPublication?.schema?.document
    check(document != null) {
      "Cannot retrieve document from $endpoint\nCheck graph id and variant"
    }
    return document
  }

  inline fun <reified T> Any?.cast() = this as? T
}
