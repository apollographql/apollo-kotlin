package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.fromJson
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema
import com.apollographql.apollo.compiler.parser.introspection.toSDL
import com.squareup.moshi.JsonWriter
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.buffer
import okio.sink
import java.io.ByteArrayOutputStream
import java.io.File
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

  private fun executeQuery(query: String, url: String, headers: Map<String, String>): Response {
    val byteArrayOutputStream = ByteArrayOutputStream()
    JsonWriter.of(byteArrayOutputStream.sink().buffer())
        .apply {
          beginObject()
          name("query")
          value(query)
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
        .url(url)
        .build()

    val response = newOkHttpClient()
        .newCall(request)
        .execute()

    if (!response.isSuccessful) {
      throw Exception("cannot get schema: ${response.code}:\n${response.body?.string()}")
    }

    return response
  }

  fun downloadIntrospection(
      endpoint: String,
      headers: Map<String, String>
  ): String {

    val response = executeQuery(introspectionQuery, endpoint, headers)

    return response.body.use { responseBody ->
      responseBody!!.string()
    }
  }

  fun downloadRegistry(graph: String, key: String, variant: String): String? {
    val registryQuery = """
    query {
      service(id: "$graph") {
        schema(tag: "$variant") {
          document
        }
      }
    }
  """.trimIndent()

    val response = executeQuery(registryQuery, "https://engine-graphql.apollographql.com/api/graphql", mapOf("x-api-key" to key))

    return response.body.use {
      it!!.byteStream().fromJson<Map<String, *>>()
          .get("data").cast<Map<String, *>>()
          ?.get("service").cast<Map<String, *>>()
          ?.get("schema").cast<Map<String, *>>()
          ?.get("document").cast<String>()
    }!!
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