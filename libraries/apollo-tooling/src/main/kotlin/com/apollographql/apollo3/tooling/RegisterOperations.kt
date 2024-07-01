package com.apollographql.apollo.tooling

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.ast.GQLArgument
import com.apollographql.apollo.ast.GQLDefinition
import com.apollographql.apollo.ast.GQLDirective
import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFloatValue
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLFragmentSpread
import com.apollographql.apollo.ast.GQLInlineFragment
import com.apollographql.apollo.ast.GQLIntValue
import com.apollographql.apollo.ast.GQLNode
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.GQLSelection
import com.apollographql.apollo.ast.GQLStringValue
import com.apollographql.apollo.ast.GQLVariableDefinition
import com.apollographql.apollo.ast.NodeContainer
import com.apollographql.apollo.ast.SDLWriter
import com.apollographql.apollo.ast.TransformResult
import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.ast.toUtf8
import com.apollographql.apollo.ast.transform
import com.apollographql.apollo.compiler.APOLLO_VERSION
import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import com.apollographql.apollo.tooling.platformapi.internal.RegisterOperationsMutation
import com.apollographql.apollo.tooling.platformapi.internal.type.RegisteredClientIdentityInput
import com.apollographql.apollo.tooling.platformapi.internal.type.RegisteredOperationInput
import kotlinx.coroutines.runBlocking
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
          val lineString = gqlNode.copy(directives = emptyList(), selections = emptyList()).toUtf8()
          if (lineString.length > 80) {
            write(lineString.replace(", ", " "))
          } else {
            write(lineString)
          }
          if (gqlNode.directives.isNotEmpty()) {
            write(" ")
            gqlNode.directives.join(this)
          }
          if (gqlNode.selections.isNotEmpty()) {
            write(" ")
            write("{")
            gqlNode.selections.join(this)
            write("}")
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

private fun GQLNode.copyWithSortedChildren(): GQLNode {
  return when (this) {
    is GQLDocument -> {
      copy(definitions = definitions.sortedBy { it.score() })
    }

    is GQLOperationDefinition -> {
      copy(variableDefinitions = variableDefinitions.sortedBy { it.score() }, selections = selections.sortedBy { it.score() })
    }

    is GQLField -> {
      copy(arguments = arguments.sortedBy { it.score() }, selections = selections.sortedBy { it.score() })
    }

    is GQLFragmentSpread -> {
      copy(directives = directives.sortedBy { it.score() })
    }

    is GQLInlineFragment -> {
      copy(directives = directives.sortedBy { it.score() }, selections = selections.sortedBy { it.score() })
    }

    is GQLFragmentDefinition -> {
      copy(directives = directives.sortedBy { it.score() }, selections = selections.sortedBy { it.score() })
    }

    is GQLDirective -> {
      copy(arguments = arguments.sortedBy { it.score() })
    }

    else -> this
  }
}

private fun GQLNode.sort(): GQLNode {
  val newChildren = children.mapNotNull { it.sort() }
  val nodeContainer = NodeContainer(newChildren)
  return copyWithNewChildrenInternal(nodeContainer).also {
    nodeContainer.assert()
  }.copyWithSortedChildren()
}

@ApolloExperimental
object RegisterOperations {
  private fun String.normalize(): String {
    val gqlDocument = parseAsGQLDocument().getOrThrow()

    // From https://github.com/apollographql/apollo-tooling/blob/6d69f226c2e2c54b4fc0de6394d813bddfb54694/packages/apollo-graphql/src/operationId.ts#L84

    // No need to "Drop unused definition" as we include only one operation

    // hideLiterals

    val hiddenLiterals = gqlDocument.transform {
      when (it) {
        is GQLIntValue -> {
          TransformResult.Replace(it.copy(value = "0"))
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
                  value = "0"
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
     * It's not 100% exact, but it's important that it matches what apollo-tooling is doing for safelisting
     *
     * Especially:
     * - it doesn't sort inline fragment
     * - it doesn't sort field directives
     */
    val sortedDocument = hiddenLiterals!!.sort()

    return printDocument(sortedDocument)
        .replace(Regex("\\s+"), " ")
        .replace(Regex("([^_a-zA-Z0-9]) ")) { it.groupValues[1] }
        .replace(Regex(" ([^_a-zA-Z0-9])")) { it.groupValues[1] }
  }

  fun String.safelistingHash(): String {
    return Buffer().writeUtf8(normalize()).readByteString().sha256().hex()
  }

  @Deprecated("Use persisted queries and publishOperations instead. See https://www.apollographql.com/docs/graphos/operations/persisted-queries/")
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  fun registerOperations(
      key: String,
      graphID: String,
      graphVariant: String,
      operationOutput: OperationOutput,
  ) {
    val apolloClient = newInternalPlatformApiApolloClient(apiKey = key)

    val call = apolloClient.mutation(
        RegisterOperationsMutation(
            id = graphID,
            clientIdentity = RegisteredClientIdentityInput(
                name = "apollo-kotlin",
                identifier = "apollo-kotlin",
                version = Optional.present(APOLLO_VERSION),
            ),
            operations = operationOutput.entries.map {
              val document = it.value.source
              RegisteredOperationInput(
                  signature = document.safelistingHash(),
                  document = Optional.present(document)
              )
            },
            manifestVersion = 2,
            graphVariant = graphVariant,
        )
    )

    val response = runBlocking { call.execute() }
    val data = response.data
    if (data == null) {
      throw response.toException("Cannot push operations")
    }

    val service = data.service
    if (service == null) {
      throw Exception("Cannot push operations: cannot find service '$graphID': ${response.errors?.joinToString { it.message }}")
    }

    val errors = data.service.registerOperationsWithResponse.invalidOperations.flatMap {
      it.errors ?: emptyList()
    }

    if (errors.isNotEmpty()) {
      throw Exception("Cannot push operations:\n${errors.joinToString("\n")}")
    }
    println("Operations pushed successfully")
  }
}
