package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.child
import com.squareup.moshi.JsonWriter
import okhttp3.*
import okio.Okio
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream

open class ApolloDownloadSchemaTask : DefaultTask() {
  @Input
  var endpointUrl: String? = null

  @Input
  var schemaFilePath: String? = null

  @Optional
  @Input
  var headers: Map<String, String>? = null

  @Optional
  @Input
  var queryParameters: Map<String, String>? = null

  @TaskAction
  fun taskAction() {
    if (schemaFilePath == null) {
      throw IllegalArgumentException("you need to define service.schemaFilePath.")
    }

    val byteArrayOutputStream = ByteArrayOutputStream()
    val writer = JsonWriter.of(Okio.buffer(Okio.sink(byteArrayOutputStream)))
    writer.beginObject()
    writer.name("query")
    writer.value(introspectionQuery)
    writer.endObject()
    writer.flush()

    val body = RequestBody.create(MediaType.parse("application/json"), byteArrayOutputStream.toByteArray())
    val requestBuilder = Request.Builder()
        .post(body)

    headers?.entries?.forEach {
      requestBuilder.addHeader(it.key, it.value)
    }

    val urlBuilder = HttpUrl.get(endpointUrl!!).newBuilder()
    queryParameters?.entries?.forEach {
      urlBuilder.addQueryParameter(it.key, it.value)
    }

    requestBuilder.url(urlBuilder.build())
        .url(urlBuilder.build())
        .build()

    val response = OkHttpClient().newCall(requestBuilder.build()).execute()

    if (!response.isSuccessful) {
      throw Exception("cannot get schema: ${response.code()}:\n${response.body()?.string()}")
    }

    project.projectDir.child(schemaFilePath!!).writeText(response.body()!!.string())
  }

  companion object {
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
}