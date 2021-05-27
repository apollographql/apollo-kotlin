package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.fromJson

object SchemaDownloader {
  fun downloadIntrospection(
      endpoint: String,
      headers: Map<String, String>
  ): String {

    val body = mapOf(
        "query" to introspectionQuery,
        "operationName" to "IntrospectionQuery"
    )
    val response = SchemaHelper.executeQuery(body, endpoint, headers)

    return response.body.use { responseBody ->
      responseBody!!.string()
    }
  }

  fun downloadRegistry(
      key: String,
      graph: String,
      variant: String,
      endpoint: String = "https://graphql.api.apollographql.com/api/graphql"
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

    val response = SchemaHelper.executeQuery(query, variables, endpoint, mapOf("x-api-key" to key))

    val responseString = response.body.use { it?.string() }

    val document = responseString
        ?.fromJson<Map<String, *>>()
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

  private val introspectionQuery = """
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
          args {
            ...InputValue
          }
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
        args {
          ...InputValue
        }
        type {
          ...TypeRef
        }
        isDeprecated
        deprecationReason
      }
      inputFields {
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