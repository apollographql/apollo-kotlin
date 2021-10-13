package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.OperationIdGenerator
import com.apollographql.apollo.compiler.fromJson
import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import com.apollographql.apollo.gradle.internal.SchemaDownloader.cast
import graphql.language.AstPrinter
import graphql.language.AstSorter
import graphql.language.AstTransformer
import graphql.language.FloatValue
import graphql.language.IntValue
import graphql.language.Node
import graphql.language.NodeVisitorStub
import graphql.language.StringValue
import graphql.parser.Parser
import graphql.util.TraversalControl
import graphql.util.TraverserContext
import graphql.util.TreeTransformerUtil.changeNode
import java.math.BigInteger

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

  private fun String.normalize(): String {
    val document = Parser.parse(this)

    // From https://github.com/apollographql/apollo-tooling/blob/6d69f226c2e2c54b4fc0de6394d813bddfb54694/packages/apollo-graphql/src/operationId.ts#L84

    // No need to "Drop unused definition" as we only include only one operation

    // hideLiterals

    val hiddenLiterals = AstTransformer().transform(document, object : NodeVisitorStub() {
      override fun visitIntValue(node: IntValue?, context: TraverserContext<Node<*>>?): TraversalControl {

        return changeNode(context, node!!.transform {
          it.value(0)
        })
      }

      override fun visitFloatValue(node: FloatValue?, context: TraverserContext<Node<*>>?): TraversalControl {
        /**
         * Because in JS (0.0).toString == "0" (vs "0.0" on the JVM), we replace the FloatValue by an IntValue
         * Since we always hide literals, this should be correct
         * See https://youtrack.jetbrains.com/issue/KT-33358
         */
        return changeNode(context, IntValue(BigInteger.valueOf(0)))
      }

      override fun visitStringValue(node: StringValue?, context: TraverserContext<Node<*>>?): TraversalControl {
        return changeNode(context, node!!.transform {
          it.value("")
        })
      }
    })

    val sortedDocument = AstSorter().sort(hiddenLiterals)

    return AstPrinter.printAst(sortedDocument)
        .replace(Regex("\\s+"), " ")
        .replace(Regex("([^_a-zA-Z0-9]) ")) { it.groupValues[1] }
        .replace(Regex(" ([^_a-zA-Z0-9])")) { it.groupValues[1] }
  }

  internal fun String.safelistingHash(): String {
    return OperationIdGenerator.Sha256().apply(normalize(), "")
  }

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
          val document = it.value.source
          mapOf(
              "signature" to document.safelistingHash(),
              "document" to document
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