package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.operationoutput.OperationOutput

object RegisterOperations {
  private val mutation = """
      mutation RegisterOperations(
            ${'$'}id    : ID!
            ${'$'}clientIdentity    : RegisteredClientIdentityInput!
            ${'$'}operations    : [RegisteredOperationInput!]!
            ${'$'}manifestVersion    : Int!
            ${'$'}graphVariant    : String
      ) {
        service(id:     ${'$'}id    ) {
          registerOperationsWithResponse(
            clientIdentity:     ${'$'}clientIdentity
            operations:     ${'$'}operations
            manifestVersion:     ${'$'}manifestVersion
            graphVariant:     ${'$'}graphVariant
          ) {
            invalidOperations {
              errors {
                message
              }
              signature
            }
            newOperations {
              signature
            }
            registrationSuccess
          }
        }
      }
  """.trimIndent()

  fun registerOperations(
      key: String,
      graphID: String,
      graphVariant: String,
      operationOutput: OperationOutput
  ) {
    val variables = mapOf(
        "id" to graphID,
        "clientIdentity" to mapOf(
            "name" to "apollo-android",
            "identifier" to "apollo-android",
            "version" to com.apollographql.apollo.compiler.VERSION,
        ),
        "operations" to operationOutput.entries.map {
          mapOf(
              "signature" to it.key,
              "document" to it.value.source
          )
        },
        "manifestVersion" to 2,
        "graphVariant" to graphVariant
    )

    val response = SchemaHelper.executeQuery(mutation, variables, "https://graphql.api.apollographql.com/api/graphql", mapOf("x-api-key" to key))

    check(response.isSuccessful)

    val responseString = response.body.use { it?.string() }

    println("got response $responseString")
//    val document = responseString
//        ?.fromJson<Map<String, *>>()
//        ?.get("data").cast<Map<String, *>>()
//        ?.get("service").cast<Map<String, *>>()
//        ?.get("variant").cast<Map<String, *>>()
//        ?.get("activeSchemaPublish").cast<Map<String, *>>()
//        ?.get("schema").cast<Map<String, *>>()
//        ?.get("document").cast<String>()
//
//    check(document != null) {
//      "Cannot retrieve document from $responseString\nCheck graph id and variant"
//    }
//    return document

  }
}