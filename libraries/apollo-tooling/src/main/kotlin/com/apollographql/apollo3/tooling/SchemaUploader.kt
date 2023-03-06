package com.apollographql.apollo3.tooling

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.tooling.SchemaDownloader.cast
import kotlinx.serialization.json.Json

@ApolloExperimental
object SchemaUploader {
  fun uploadSchema(
      sdl: String,
      key: String,
      graph: String?,
      variant: String = "current",
      headers: Map<String, String> = emptyMap(),
  ) {
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
    val graph2 = graph ?: key.getGraph() ?: error("graph not found")

    val variables = mapOf(
        "graphID" to graph2,
        "variant" to variant,
        "schemaDocument" to sdl,
    )

    val response = SchemaHelper.executeQuery(
        query,
        variables,
        "https://graphql.api.apollographql.com/api/graphql",
        mapOf("x-api-key" to key) + headers
    )

    val responseString = response.body.use { it?.string() }

    val uploadSchema = responseString
        ?.let { Json.parseToJsonElement(it) }
        ?.toAny().cast<Map<String, *>>()
        ?.get("data").cast<Map<String, *>>()
        ?.get("service").cast<Map<String, *>>()
        ?.get("uploadSchema").cast<Map<String, *>>()

    check(uploadSchema?.get("success") == true) {
      "Cannot upload schema: ${uploadSchema?.get("message")}"
    }
  }
}