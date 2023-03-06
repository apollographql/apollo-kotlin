package com.apollographql.apollo3.tooling

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.introspection.IntrospectionSchema
import com.apollographql.apollo3.ast.introspection.toGQLDocument
import com.apollographql.apollo3.ast.introspection.toIntrospectionSchema
import com.apollographql.apollo3.ast.introspection.writeTo
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.toUtf8
import com.apollographql.apollo3.ast.validateAsSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import okio.Buffer
import java.io.File

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
        introspectionSchemaJson = try {
          downloadIntrospection(
              endpoint = endpoint,
              headers = headers,
              insecure = insecure,
              includeDeprecatedInputFieldsAndArguments = true,
          )
        } catch (e: Exception) {
          // Maybe the server doesn't support deprecated input fields / arguments, try without them
          downloadIntrospection(
              endpoint = endpoint,
              headers = headers,
              insecure = insecure,
              includeDeprecatedInputFieldsAndArguments = false,
          )
        }
        introspectionSchema = introspectionSchemaJson!!.toIntrospectionSchema()
      }
      else -> {
        check(key != null) {
          "Apollo: either endpoint (for introspection) or key (for registry) is required"
        }
        val graph2 = graph ?: key.getGraph()
        check (graph2 != null) {
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
      includeDeprecatedInputFieldsAndArguments: Boolean,
  ): String {

    val body = mapOf(
        "query" to getIntrospectionQuery(includeDeprecatedInputFieldsAndArguments),
        "operationName" to "IntrospectionQuery"
    )
    val response = SchemaHelper.executeQuery(body, endpoint, headers, insecure)

    return response.body.use { responseBody ->
      responseBody!!.string()
    }
  }

  fun downloadRegistry(
      key: String,
      graph: String,
      variant: String,
      endpoint: String = "https://graphql.api.apollographql.com/api/graphql",
      headers: Map<String, String>,
      insecure: Boolean,
  ): String {
    val query = """
    query DownloadSchema(${'$'}graphID: ID!, ${'$'}variant: String!) {
      service(id: ${'$'}graphID) {
        variant(name: ${'$'}variant) {
          activeSchemaPublish {
            schema {
              document
            }
          }
        }
      }
    }
  """.trimIndent()
    val variables = mapOf("graphID" to graph, "variant" to variant)

    val response = SchemaHelper.executeQuery(query, variables, endpoint, headers + mapOf("x-api-key" to key), insecure)

    val responseString = response.body.use { it?.string() }

    val document = responseString
        ?.let { Json.parseToJsonElement(it) }
        ?.toAny().cast<Map<String, *>>()
        ?.get("data").cast<Map<String, *>>()
        ?.get("service").cast<Map<String, *>>()
        ?.get("variant").cast<Map<String, *>>()
        ?.get("activeSchemaPublish").cast<Map<String, *>>()
        ?.get("schema").cast<Map<String, *>>()
        ?.get("document").cast<String>()

    check(document != null) {
      "Cannot retrieve document from $responseString\nCheck graph id and variant"
    }
    return document
  }

  inline fun <reified T> Any?.cast() = this as? T

  private fun getIntrospectionQuery(includeDeprecatedInputFieldsAndArguments: Boolean): String {
    val includeDeprecated = if (includeDeprecatedInputFieldsAndArguments) "(includeDeprecated: true)" else ""
    return """
      query IntrospectionQuery {
        __schema {
          queryType { name }
          mutationType { name }
          subscriptionType { name }
          types {
            ...FullType
          }
          directives {
            name
            description
            locations
            args$includeDeprecated {
              ...InputValue
            }
            isRepeatable
          }
        }
      }
  
      fragment FullType on __Type {
        kind
        name
        description
        fields(includeDeprecated: true) {
          name
          description
          args$includeDeprecated {
            ...InputValue
          }
          type {
            ...TypeRef
          }
          isDeprecated
          deprecationReason
        }
        inputFields$includeDeprecated {
          ...InputValue
        }
        interfaces {
          ...TypeRef
        }
        enumValues(includeDeprecated: true) {
          name
          description
          isDeprecated
          deprecationReason
        }
        possibleTypes {
          ...TypeRef
        }
      }
  
      fragment InputValue on __InputValue {
        name
        description
        type { ...TypeRef }
        defaultValue
        isDeprecated
        deprecationReason
      }
  
      fragment TypeRef on __Type {
        kind
        name
        ofType {
          kind
          name
          ofType {
            kind
            name
            ofType {
              kind
              name
              ofType {
                kind
                name
                ofType {
                  kind
                  name
                  ofType {
                    kind
                    name
                    ofType {
                      kind
                      name
                    }
                  }
                }
              }
            }
          }
        }
      }""".trimIndent()
  }
}
