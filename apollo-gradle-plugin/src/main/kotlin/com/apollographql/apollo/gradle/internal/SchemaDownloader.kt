package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.fromJson
import com.squareup.moshi.JsonWriter
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.buffer
import okio.sink
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object SchemaDownloader {
  private fun newOkHttpClient(): OkHttpClient {
    val connectTimeoutSeconds = System.getProperty("okHttp.connectTimeout", "600").toLong()
    val readTimeoutSeconds = System.getProperty("okHttp.readTimeout", "600").toLong()
    return OkHttpClient.Builder()
        .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
        .build()
  }

  private fun executeQuery(query: String, variables: String? = null, url: String, headers: Map<String, String>): Response {
    val byteArrayOutputStream = ByteArrayOutputStream()
    JsonWriter.of(byteArrayOutputStream.sink().buffer())
        .apply {
          beginObject()
          name("query")
          value(query)
          if (variables != null) {
            name("variables")
            value(variables)
          }
          endObject()
          flush()
        }

    val body = byteArrayOutputStream.toByteArray().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder()
        .post(body)
        .apply {
          headers.entries.forEach {
            addHeader(it.key, it.value)
          }
        }
        .header("apollographql-client-name", "apollo-gradle-plugin")
        .header("apollographql-client-version", com.apollographql.apollo.compiler.VERSION)
        .url(url)
        .build()

    val response = newOkHttpClient()
        .newCall(request)
        .execute()

    check(response.isSuccessful) {
      "cannot get schema from $url: ${response.code}:\n${response.body?.string()}"
    }

    return response
  }

  fun downloadIntrospection(
      endpoint: String,
      headers: Map<String, String>
  ): String {

    val response = executeQuery(introspectionQuery, null, endpoint, headers)

    return response.body.use { responseBody ->
      responseBody!!.string()
    }
  }

  fun downloadRegistry(graph: String, key: String, variant: String): String? {
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
    val variables = """
      {
        "graphID": "$graph",
        "variant": "$variant"
      }
    """.trimIndent()

    val response = executeQuery(query, variables, "https://graphql.api.apollographql.com/api/graphql", mapOf("x-api-key" to key))

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

  val introspectionQuery = """
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