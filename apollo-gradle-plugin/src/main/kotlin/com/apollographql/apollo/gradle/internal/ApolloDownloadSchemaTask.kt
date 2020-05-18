package com.apollographql.apollo.gradle.internal

import com.squareup.moshi.JsonWriter
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.buffer
import okio.sink
import org.gradle.api.DefaultTask
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

abstract class ApolloDownloadSchemaTask : DefaultTask() {
  @get:Input
  abstract val endpointUrl: Property<String>

  @get:Input
  abstract val schemaFilePath: Property<String>

  @get:Optional
  @get:Input
  abstract val headers: MapProperty<String, String>

  @get:Optional
  @get:Input
  abstract val queryParameters: MapProperty<String, String>

  init {
    /**
     * We cannot know in advance if the backend schema changed so don't cache or mark this task up-to-date
     * This code actually redundant because the task has no output but adding it make it explicit.
     */
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
  }

  @TaskAction
  fun taskAction() {
    if (!schemaFilePath.isPresent) {
      throw IllegalArgumentException("you need to define schemaFilePath.")
    }

    val byteArrayOutputStream = ByteArrayOutputStream()
    val writer = JsonWriter.of(byteArrayOutputStream.sink().buffer())
    writer.beginObject()
    writer.name("query")
    writer.value(introspectionQuery)
    writer.endObject()
    writer.flush()

    val body = byteArrayOutputStream.toByteArray().toRequestBody("application/json".toMediaTypeOrNull())
    val requestBuilder = Request.Builder()
        .post(body)

    headers.get().entries.forEach {
      requestBuilder.addHeader(it.key, it.value)
    }

    val urlBuilder = endpointUrl.get().toHttpUrl().newBuilder()
    queryParameters.get().entries.forEach {
      urlBuilder.addQueryParameter(it.key, it.value)
    }

    requestBuilder.url(urlBuilder.build())
        .url(urlBuilder.build())
        .build()

    val response = OkHttpClient.Builder()
        .connectTimeout(System.getProperty("okHttp.connectTimeout", "60").toLong(), TimeUnit.SECONDS)
        .readTimeout(System.getProperty("okHttp.readTimeout", "60").toLong(), TimeUnit.SECONDS)
        .build()
        .newCall(requestBuilder.build()).execute()

    if (!response.isSuccessful) {
      throw Exception("cannot get schema: ${response.code}:\n${response.body?.string()}")
    }

    val jsonFile = project.projectDir.child(schemaFilePath.get())

    jsonFile.parentFile.mkdirs()
    jsonFile.writeText(response.body!!.string())
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
