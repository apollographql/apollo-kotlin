package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.toJson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

internal object SchemaHelper {
  private fun newOkHttpClient(): OkHttpClient {
    val connectTimeoutSeconds = System.getProperty("okHttp.connectTimeout", "600").toLong()
    val readTimeoutSeconds = System.getProperty("okHttp.readTimeout", "600").toLong()
    return OkHttpClient.Builder()
        .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
        .addInterceptor { chain ->
          chain.request().newBuilder()
              .header("apollographql-client-name", "apollo-gradle-plugin")
              .header("apollographql-client-version", com.apollographql.apollo3.compiler.APOLLO_VERSION)
              .build()
              .let {
                chain.proceed(it)
              }

        }
        .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
        .build()
  }
  internal fun executeQuery(map: Map<String, Any?>, url: String, headers: Map<String, String>): Response {
    val body = map.toJson().toByteArray().toRequestBody("application/json".toMediaTypeOrNull())
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

    check(response.isSuccessful) {
      "cannot get schema from $url: ${response.code}:\n${response.body?.string()}"
    }

    return response
  }
  /**
   * @param variables a map representing the variable as Json values
   */
  internal fun executeQuery(query: String, variables: Map<String, Any>, url: String, headers: Map<String, String>): Response {
    return executeQuery(mapOf("query" to query, "variables" to variables), url, headers)
  }
}