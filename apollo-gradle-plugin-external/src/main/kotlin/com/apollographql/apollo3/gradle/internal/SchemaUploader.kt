package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.fromJson
import com.apollographql.apollo3.gradle.internal.SchemaDownloader.cast

object SchemaUploader {
  fun uploadSchema(key: String, graph: String, variant: String, sdl: String) {
    val query = """
    mutation UploadSchema(${'$'}graphID: ID!,  ${'$'}variant: String!, ${'$'}schemaDocument: String!) {
      service(id: ${'$'}graphID) {
        
        uploadSchema(schemaDocument: ${'$'}schemaDocument, tag: ${'$'}variant) {
          success
          message
        }
      }
    }
  """.trimIndent()
    val variables = mapOf(
        "graphID" to graph,
        "variant" to variant,
        "schemaDocument" to sdl,
    )

    val response = SchemaHelper.executeQuery(query, variables, "https://graphql.api.apollographql.com/api/graphql", mapOf("x-api-key" to key))

    val responseString = response.body.use { it?.string() }

    val uploadSchema = responseString
        ?.fromJson<Map<String, *>>()
        ?.get("data").cast<Map<String, *>>()
        ?.get("service").cast<Map<String, *>>()
        ?.get("uploadSchema").cast<Map<String, *>>()

    check(uploadSchema?.get("success") == true) {
      "Cannot upload schema: ${uploadSchema?.get("message")}"
    }
  }
}