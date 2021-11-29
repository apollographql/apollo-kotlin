package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.GQLArgument
import com.apollographql.apollo3.ast.GQLArguments
import com.apollographql.apollo3.ast.GQLDefinition
import com.apollographql.apollo3.ast.GQLDirective
import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFloatValue
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLIntValue
import com.apollographql.apollo3.ast.GQLNode
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.GQLSelection
import com.apollographql.apollo3.ast.GQLSelectionSet
import com.apollographql.apollo3.ast.GQLStringValue
import com.apollographql.apollo3.ast.GQLVariableDefinition
import com.apollographql.apollo3.ast.NodeContainer
import com.apollographql.apollo3.ast.SDLWriter
import com.apollographql.apollo3.ast.TransformResult
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.toUtf8
import com.apollographql.apollo3.ast.transform
import com.apollographql.apollo3.compiler.APOLLO_VERSION
import com.apollographql.apollo3.compiler.OperationIdGenerator
import com.apollographql.apollo3.compiler.fromJson
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import com.apollographql.apollo3.gradle.internal.SchemaDownloader.cast
import okio.Buffer


private fun GQLDefinition.score(): String {
  // Fragments always go first
  return when (this) {
    is GQLFragmentDefinition -> "a$name"
    is GQLOperationDefinition -> "b$name"
    else -> error("Cannot sort Definition '$this'")
  }
}

private fun GQLVariableDefinition.score(): String {
  return this.name
}

private fun GQLSelection.score(): String {
  return when (this) {
    is GQLField -> "a$name"
    is GQLFragmentSpread -> "b$name"
    is GQLInlineFragment -> "c" // apollo-tooling doesn't sort inline fragments
    else -> error("Cannot sort Selection '$this'")
  }
}

private fun GQLDirective.score() = name

private fun GQLArgument.score() = name

private fun isEmpty(s: String?): Boolean {
  return s == null || s.trim { it <= ' ' }.length == 0
}

@OptIn(ApolloExperimental::class)
private fun <T : GQLNode> List<T>.join(
    writer: SDLWriter,
    separator: String = " ",
    prefix: String = "",
    postfix: String = "",
    block: (T) -> Unit = { writer.write(it) },
) {
  writer.write(prefix)
  forEachIndexed { index, t ->
    block(t)
    if (index < size - 1) {
      writer.write(separator)
    }
  }
  writer.write(postfix)
}

/**
 * A document printer that can use " " as a separator for field arguments when the line becomes bigger than 80 chars
 * as in graphql-js: https://github.com/graphql/graphql-js/blob/6453612a6c40a1f8ad06845f1516c5f0dafa666c/src/language/printer.ts#L62
 */
@OptIn(ApolloExperimental::class)
private fun printDocument(gqlNode: GQLNode): String {
  val buffer = Buffer()

  val writer = object : SDLWriter(buffer, "  ") {
    override fun write(gqlNode: GQLNode) {
      when (gqlNode) {
        is GQLField -> {
          /**
           * Compute the size of "$alias: $name(arg1: value1, arg2: value2, etc...)"
           *
           * If it's bigger than 80, replace ', ' with ' '
           */
          val lineString = gqlNode.copy(directives = emptyList(), selectionSet = null).toUtf8()
          if (lineString.length > 80) {
            write(lineString.replace(", ", " "))
          } else {
            write(lineString)
          }
          if (gqlNode.directives.isNotEmpty()) {
            write(" ")
            gqlNode.directives.join(this)
          }
          if (gqlNode.selectionSet != null) {
            write(" ")
            write(gqlNode.selectionSet!!)
          } else {
            write("\n")
          }
        }
        else -> super.write(gqlNode)
      }
    }
  }

  writer.write(gqlNode)

  return buffer.readUtf8()
}

@OptIn(ApolloExperimental::class)
fun GQLNode.copyWithSortedChildren(): GQLNode {
  return when (this) {
    is GQLDocument -> {
      copy(definitions = definitions.sortedBy { it.score() })
    }
    is GQLOperationDefinition -> {
      copy(variableDefinitions = variableDefinitions.sortedBy { it.score() })
    }
    is GQLSelectionSet -> {
      copy(selections = selections.sortedBy { it.score() })
    }
    is GQLField -> {
      copy(arguments = arguments)
    }
    is GQLFragmentSpread -> {
      copy(directives = directives.sortedBy { it.score() })
    }
    is GQLInlineFragment -> {
      copy(directives = directives.sortedBy { it.score() })
    }
    is GQLFragmentDefinition -> {
      copy(directives = directives.sortedBy { it.score() })
    }
    is GQLDirective -> {
      copy(arguments = arguments)
    }
    is GQLArguments -> {
      copy(arguments = arguments.sortedBy { it.score() })
    }
    else -> this
  }
}

@OptIn(ApolloExperimental::class)
fun GQLNode.sort(): GQLNode {
  val newChildren = children.mapNotNull { it.sort() }
  val nodeContainer = NodeContainer(newChildren)
  return copyWithNewChildrenInternal(nodeContainer).also {
    nodeContainer.assert()
  }.copyWithSortedChildren()
}

@OptIn(ApolloExperimental::class)
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
    val gqlDocument = Buffer().writeUtf8(this).parseAsGQLDocument().valueAssertNoErrors()

    // From https://github.com/apollographql/apollo-tooling/blob/6d69f226c2e2c54b4fc0de6394d813bddfb54694/packages/apollo-graphql/src/operationId.ts#L84

    // No need to "Drop unused definition" as we include only one operation

    // hideLiterals

    val hiddenLiterals = gqlDocument.transform {
      when (it) {
        is GQLIntValue -> {
          TransformResult.Replace(it.copy(value = 0))
        }
        is GQLFloatValue -> {
          /**
           * Because in JS (0.0).toString == "0" (vs "0.0" on the JVM), we replace the FloatValue by an IntValue
           * Since we always hide literals, this should be correct
           * See https://youtrack.jetbrains.com/issue/KT-33358
           */
          TransformResult.Replace(
              GQLIntValue(
                  sourceLocation = it.sourceLocation,
                  value = 0
              )
          )
        }
        is GQLStringValue -> {
          TransformResult.Replace(it.copy(value = ""))
        }
        else -> TransformResult.Continue
      }
    }

    /**
     * Algorithm taken from https://github.com/apollographql/apollo-tooling/blob/6d69f226c2e2c54b4fc0de6394d813bddfb54694/packages/apollo-graphql/src/transforms.ts#L102
     * It's not 100% exact but it's important that it matches what apollo-tooling is doing for safelisting
     *
     * Especially:
     * - it doesn't sort inline fragment
     * - it doesn't sort field directives
     */
    val sortedDocument = hiddenLiterals!!.sort()

    val minimized = printDocument(sortedDocument)
        .replace(Regex("\\s+"), " ")
        .replace(Regex("([^_a-zA-Z0-9]) ")) { it.groupValues[1] }
        .replace(Regex(" ([^_a-zA-Z0-9])")) { it.groupValues[1] }

    return minimized
  }

  internal fun String.safelistingHash(): String {
    return OperationIdGenerator.Sha256.apply(normalize(), "")
  }

  fun registerOperations(
      key: String,
      graphID: String,
      graphVariant: String,
      operationOutput: OperationOutput,
  ) {
    val variables = mapOf(
        "id" to graphID,
        "clientIdentity" to mapOf(
            "name" to "apollo-android",
            "identifier" to "apollo-android",
            "version" to APOLLO_VERSION,
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
