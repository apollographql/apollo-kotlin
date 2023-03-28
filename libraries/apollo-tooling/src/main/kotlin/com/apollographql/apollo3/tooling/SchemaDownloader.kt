package com.apollographql.apollo3.tooling

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.introspection.IntrospectionSchema
import com.apollographql.apollo3.ast.introspection.toGQLDocument
import com.apollographql.apollo3.ast.introspection.toIntrospectionSchema
import com.apollographql.apollo3.ast.introspection.writeTo
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.toUtf8
import com.apollographql.apollo3.ast.validateAsSchema
import com.apollographql.apollo3.network.okHttpClient
import com.apollographql.apollo3.tooling.platformApi.DownloadSchemaQuery
import kotlinx.coroutines.runBlocking
import okio.Buffer
import java.io.File
import com.apollographql.apollo3.tooling.graphql.draft.IntrospectionQuery as GraphQLDraftIntrospectionQuery
import com.apollographql.apollo3.tooling.graphql.june2018.IntrospectionQuery as GraphQLJune2018IntrospectionQuery
import com.apollographql.apollo3.tooling.graphql.october2021.IntrospectionQuery as GraphQLOctober2021IntrospectionQuery

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


@ApolloExperimental
object SchemaDownloader {
  enum class SpecVersion {
    June_2018,
    October_2021,
    Draft,
  }

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
      registryUrl: String = "https://graphql.api.apollographql.com/api/graphql",
      schema: File,
      insecure: Boolean = false,
      headers: Map<String, String> = emptyMap(),
  ) {
    var introspectionSchemaJson: String? = null
    var introspectionSchema: IntrospectionSchema? = null
    var gqlSchema: GQLDocument? = null
    when {
      endpoint != null -> {
        var exception: Exception? = null
        // Try the latest spec first
        for (specVersion in SpecVersion.values().reversed()) {
          try {
            introspectionSchemaJson = downloadIntrospection(
                endpoint = endpoint,
                headers = headers,
                insecure = insecure,
                specVersion = specVersion,
            )
            // Validates the JSON schema
            introspectionSchema = introspectionSchemaJson.toIntrospectionSchema()
            exception = null
            break
          } catch (e: Exception) {
            exception = e
          }
        }
        if (introspectionSchemaJson == null) {
          throw exception!!
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

        gqlSchema = downloadRegistry(
            graph = graph2,
            key = key,
            variant = graphVariant,
            endpoint = registryUrl,
            headers = headers,
            insecure = insecure,
        ).let { Buffer().writeUtf8(it) }.parseAsGQLDocument().getOrThrow()
      }
    }

    schema.parentFile?.mkdirs()

    if (schema.extension.lowercase() == "json") {
      if (introspectionSchema == null) {
        introspectionSchema = gqlSchema!!.validateAsSchema().getOrThrow().toIntrospectionSchema()
        introspectionSchema.writeTo(schema)
      } else {
        schema.writeText(introspectionSchemaJson!!)
      }
    } else {
      if (gqlSchema == null) {
        gqlSchema = introspectionSchema!!.toGQLDocument()
      }
      schema.writeText(gqlSchema.toUtf8(indent = "  "))
    }
  }

  fun downloadIntrospection(
      endpoint: String,
      headers: Map<String, String>,
      insecure: Boolean,
      specVersion: SpecVersion,
  ): String {
    return SchemaHelper.executeQuery(
        query = when (specVersion) {
          SpecVersion.June_2018 -> GraphQLJune2018IntrospectionQuery()
          SpecVersion.October_2021 -> GraphQLOctober2021IntrospectionQuery()
          SpecVersion.Draft -> GraphQLDraftIntrospectionQuery()
        },
        endpoint = endpoint,
        headers = headers,
        insecure = insecure,
    )
  }

  fun downloadRegistry(
      key: String,
      graph: String,
      variant: String,
      endpoint: String = "https://graphql.api.apollographql.com/api/graphql",
      headers: Map<String, String>,
      insecure: Boolean,
  ): String {
    val responseString = SchemaHelper.executeQuery(
        query = DownloadSchemaQuery(graphID = graph, variant = variant),
        endpoint = endpoint,
        headers = headers + mapOf("x-api-key" to key),
        insecure = insecure
    )
    val apolloClient = ApolloClient.Builder()
        .serverUrl(endpoint)
        .okHttpClient(SchemaHelper.newOkHttpClient(insecure))
        .build()
    val response = runBlocking {
      apolloClient.query(DownloadSchemaQuery(graphID = graph, variant = variant))
          .httpHeaders(headers.map { HttpHeader(it.key, it.value) })
          .addHttpHeader("x-api-key", key)
          .execute()
    }
    val document = response.data?.service?.variant?.activeSchemaPublish?.schema?.document
    check(document != null) {
      "Cannot retrieve document from $responseString\nCheck graph id and variant"
    }
    return document
  }

  inline fun <reified T> Any?.cast() = this as? T
}
