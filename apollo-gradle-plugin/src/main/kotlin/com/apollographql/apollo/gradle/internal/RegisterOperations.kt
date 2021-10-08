package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.fromJson
import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import com.apollographql.apollo.gradle.internal.SchemaDownloader.cast
import org.jetbrains.kotlin.gradle.utils.`is`

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

    val errors = responseString
        ?.fromJson<Map<String, *>>()
        ?.get("data").cast<Map<String, *>>()
        ?.get("service").cast<Map<String, *>>()
        ?.get("registerOperationsWithResponse").cast<Map<String, *>>()
        ?.get("invalidOperations").cast<List<Map<String, *>>>()
        ?.flatMap {
          it.get("errors").cast<List<String>>() ?: emptyList()
        } ?: emptyList()

    check(errors.isEmpty()) {
      "Cannot push operations: ${errors.joinToString("\n")}"
    }

    val success = responseString
        ?.fromJson<Map<String, *>>()
        ?.get("data").cast<Map<String, *>>()
        ?.get("service").cast<Map<String, *>>()
        ?.get("registerOperationsWithResponse").cast<Map<String, *>>()
        ?.get("registrationSuccess").cast<Boolean>()
        ?: false

    check(success) {
      "Cannot push operations: $responseString"
    }

    println("Operations pushed successfully")
  }
}