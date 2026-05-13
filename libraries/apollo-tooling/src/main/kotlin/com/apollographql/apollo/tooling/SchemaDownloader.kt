package com.apollographql.apollo.tooling

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.api.internal.readErrors
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.introspection.IntrospectionSchema
import com.apollographql.apollo.ast.introspection.toGQLDocument
import com.apollographql.apollo.ast.introspection.toIntrospectionSchema
import com.apollographql.apollo.ast.introspection.writeTo
import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.ast.toFullSchemaGQLDocument
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.toSDL
import com.apollographql.apollo.ast.toUtf8
import com.apollographql.apollo.network.okHttpClient
import com.apollographql.apollo.tooling.SchemaHelper.reworkFullTypeFragment
import com.apollographql.apollo.tooling.SchemaHelper.reworkInputValueFragment
import com.apollographql.apollo.tooling.SchemaHelper.reworkIntrospectionQuery
import com.apollographql.apollo.tooling.SchemaHelper.reworkServiceCapabilities
import com.apollographql.apollo.tooling.graphql.PreIntrospectionQuery
import com.apollographql.apollo.tooling.platformapi.public.DownloadSchemaQuery
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.buffer
import okio.source
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
        try {
          introspectionDataJson = downloadIntrospection(
              endpoint = endpoint,
              headers = headers,
              insecure = insecure,
              failSafe = false,
          )
          introspectionSchema = try {
            introspectionDataJson.toIntrospectionSchema()
          } catch (e: Exception) {
            throw Exception("Introspection response from $endpoint can not be parsed (see `cause` for more details)", e)
          }
        } catch (e: Exception) {
          // 2-step introspection didn't work: fallback to no pre-introspection query and minimal introspection query
          introspectionDataJson = downloadIntrospection(
              endpoint = endpoint,
              headers = headers,
              insecure = insecure,
              failSafe = true,
          )
          introspectionSchema = try {
            introspectionDataJson.toIntrospectionSchema()
          } catch (e: Exception) {
            throw Exception("Introspection response from $endpoint can not be parsed (see `cause` for more details)", e)
          }
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
        /*
         * As of Feb. 2025, the registry schema contains the introspection types but not
         * the scalars and we therefore need to call `toFullSchemaGQLDocument()`
         *
         * TODO: Should this be removed? It's not clear what value there is in converting from SDL to JSON.
         */
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
        schema.writeText(introspectionSchema.toGQLDocument().toSDL(indent = "  ", includeBuiltInScalarDefinitions = true))
      } else {
        // Copy SDL verbatim
        schema.writeText(sdlSchema)
      }
    }
  }

  /**
   * Get an introspection query that is compatible with the given [features], as a JSON string.
   */
  @ApolloExperimental
  fun getIntrospectionQuery(features: Set<GraphQLFeature>): String {
    val baseIntrospectionSource =
      SchemaHelper::class.java.classLoader!!.getResourceAsStream("base-introspection.graphql")!!.source().buffer()
    val baseIntrospectionGql: GQLDocument = baseIntrospectionSource.parseAsGQLDocument().value!!
    val introspectionGql: GQLDocument = baseIntrospectionGql.copy(
        definitions = baseIntrospectionGql.definitions
            .reworkIntrospectionQuery(features)
            .reworkFullTypeFragment(features)
            .reworkInputValueFragment(features)
            .reworkServiceCapabilities(features)
    )
    return introspectionGql.toUtf8()
  }

  @Deprecated(level = DeprecationLevel.HIDDEN, message = "Kept for binary compatibility")
  fun downloadIntrospection(
      endpoint: String,
      headers: Map<String, String>,
      insecure: Boolean,
  ): String = downloadIntrospection(endpoint, headers, insecure, failSafe = false)

  fun downloadIntrospection(
      endpoint: String,
      headers: Map<String, String>,
      insecure: Boolean,
      failSafe: Boolean = false,
  ): String {
    var features = if (failSafe) {
      emptySet()
    } else {
      val preIntrospectionData: PreIntrospectionQuery.Data = SchemaHelper.executePreIntrospectionQuery(
          endpoint = endpoint,
          headers = headers,
          insecure = insecure,
      )
      preIntrospectionData.getFeatures()
    }

    var lastFeatures = features
    while (true) {
      val introspectionQuery = getIntrospectionQuery(features)
      val response = SchemaHelper.executeIntrospectionQuery(
          introspectionQuery = introspectionQuery,
          endpoint = endpoint,
          headers = headers,
          insecure = insecure,
      )

      response.body?.readErrors().orEmpty().forEach {
        // See https://github.com/apollographql/apollo-kotlin/issues/6945
        when (it.message) {
          "Cannot query field \"isDeprecated\" on type \"__Directive\"." -> features = features - GraphQLFeature.DeprecatedDirectives
          "Cannot query field \"isOneOf\" on type \"__Type\"." -> features = features - GraphQLFeature.OneOf
        }
      }

      if (lastFeatures == features) {
        check(response.statusCode in 200..299 && response.body != null) {
          "Cannot get schema from $endpoint: ${response.statusCode}:\n${response.body ?: "(empty body)"}"
        }
        return response.body
      } else {
        lastFeatures = features
      }
    }
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
        .httpExposeErrorBody(true)
        .build()
    val response = runBlocking {
      apolloClient.query(DownloadSchemaQuery(graphID = graph, variant = variant))
          .httpHeaders(headers.map { HttpHeader(it.key, it.value) } + HttpHeader("x-api-key", key))
          .execute()
    }
    val data = response.data

    if (data == null) {
      throw response.toException("Cannot download schema")
    }

    if (data.graph == null) {
      throw Exception("Cannot retrieve graph '$graph': ${response.errors?.joinToString { it.message }}")
    }

    if (data.graph.variant == null) {
      throw Exception("Cannot retrieve variant '$variant': ${response.errors?.joinToString { it.message }}")
    }
    return data.graph.variant.latestPublication.schema.document
  }

  fun shutdown() {
    SchemaHelper.client.dispatcher.executorService.shutdown()
    SchemaHelper.client.connectionPool.evictAll()
  }

  inline fun <reified T> Any?.cast() = this as? T
}

private fun String.readErrors(): List<Error> {
  Buffer().writeUtf8(this).jsonReader().use { reader ->
    reader.beginObject()
    while (reader.hasNext()) {
      when (reader.nextName()) {
        "errors" -> return reader.readErrors()
        else -> reader.skipValue()
      }
    }
    return emptyList()
  }
}