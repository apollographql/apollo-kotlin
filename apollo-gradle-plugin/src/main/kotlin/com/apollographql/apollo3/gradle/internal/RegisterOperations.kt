package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.ast.GQLArgument
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
import com.apollographql.apollo3.ast.TransformResult
import com.apollographql.apollo3.ast.toGQLDocument
import com.apollographql.apollo3.ast.toUtf8
import com.apollographql.apollo3.ast.transform
import com.apollographql.apollo3.compiler.OperationIdGenerator
import com.apollographql.apollo3.compiler.fromJson
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import com.apollographql.apollo3.gradle.internal.SchemaDownloader.cast

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.math.BigInteger

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

private fun wrap(start: String, maybeString: String?, end: String): String {
  return if (isEmpty(maybeString)) {
    if (start == "\"" && end == "\"") {
      "\"\""
    } else ""
  } else start + maybeString + if (!isEmpty(end)) end else ""
}

private fun <T : Node<*>> join(
    nodes: List<T>,
    delim: String,
    nodeMethod: (Node<*>) -> String,
    prefix: String = "",
    suffix: String = "",
): String {
  val joined = StringBuilder()
  joined.append(prefix)
  var first = true
  for (node in nodes) {
    if (first) {
      first = false
    } else {
      joined.append(delim)
    }
    joined.append(nodeMethod(node))
  }
  joined.append(suffix)
  return joined.toString()
}

/**
 * A document printer that can use " " as a separator for field arguments when the line becomes bigger than 80 chars
 * as in graphql-js: https://github.com/graphql/graphql-js/blob/6453612a6c40a1f8ad06845f1516c5f0dafa666c/src/language/printer.ts#L62
 */
private fun printDocument(node: GQLNode): String {
  node.toUtf8()
  val printerCtor = AstPrinter::class.java.getDeclaredConstructor(Boolean::class.java).apply {
    isAccessible = true
  }
  val printer = printerCtor.newInstance(false)

  val nodePrinterClass = Class.forName("graphql.language.AstPrinter${'$'}NodePrinter")
  val printers = AstPrinter::class.java.getDeclaredField("printers").apply {
    isAccessible = true
  }.get(printer) as LinkedHashMap<Class<*>, Any>

  val directivesMethod = AstPrinter::class.java.getDeclaredMethod("directives", List::class.java).apply {
    isAccessible = true
  }
  val nodeMethod = AstPrinter::class.java.getDeclaredMethod("node", Node::class.java).apply {
    isAccessible = true
  }
  val fieldPrinter = Proxy.newProxyInstance(
      AstPrinter::class.java.classLoader,
      arrayOf(nodePrinterClass),
      InvocationHandler { proxy, method, args ->
        check(method.name == "print")
        val out = args[0] as StringBuilder
        val node = args[1] as Field

        /**
         * This code is a copy/paste from graphql-java. It could be simplified but keeping it
         * closer to graphql-java will make it easier to follow changes if any
         */
        val compactMode = false
        val argSep = if (compactMode) "," else ", "
        val aliasSuffix = if (compactMode) ":" else ": "

        val alias: String = wrap("", node.alias, aliasSuffix)
        val name: String = node.name
        var arguments: String = wrap("(", join<Argument>(node.arguments, argSep, { nodeMethod.invoke(printer, it) as String }), ")")
        if (arguments.length > 80) {
          arguments = wrap("(", join<Argument>(node.arguments, " ", { nodeMethod.invoke(printer, it) as String }), ")")
        }
        val directives: String = directivesMethod.invoke(printer, node.directives) as String
        val selectionSet: String = nodeMethod.invoke(printer, node.selectionSet) as String

        out.append(
            alias + name + arguments,
            directives,
            selectionSet
        )

        Unit
      }
  )

  printers[Field::class.java] = fieldPrinter

  val documentPrinter = AstPrinter::class.java.getDeclaredMethod("_findPrinter", Node::class.java).apply {
    isAccessible = true
  }.invoke(printer, document)

  val builder = StringBuilder()
  nodePrinterClass.getDeclaredMethod("print", StringBuilder::class.java, Node::class.java).apply {
    isAccessible = true
  }.invoke(documentPrinter, builder, document)

  return builder.toString()
}

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
    val gqlDocument = this.toGQLDocument()

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
    val sortedDocument = hiddenLiterals!!.transform { gqlNode ->
      when (gqlNode) {
        is GQLDocument -> {
          TransformResult.Replace(
              gqlNode.copy(
                  definitions = gqlNode.definitions.sortedBy { it.score() }
              )
          )
        }
        is GQLOperationDefinition -> {
          TransformResult.Replace(
              gqlNode.copy(
                  variableDefinitions = gqlNode.variableDefinitions.sortedBy { it.score() }
              )
          )
        }
        is GQLSelectionSet -> {
          TransformResult.Replace(
              gqlNode.copy(
                  selections = gqlNode.selections.sortedBy { it.score() }
              )
          )
        }
        is GQLField -> {
          TransformResult.Replace(
              gqlNode.copy(
                  arguments = gqlNode.arguments?.copy(gqlNode.arguments!!.arguments.sortedBy { it.score() })
              )
          )
        }
        is GQLFragmentSpread -> {
          TransformResult.Replace(
              gqlNode.copy(
                  directives = gqlNode.directives.sortedBy { it.score() }
              )
          )
        }
        is GQLInlineFragment -> {
          TransformResult.Replace(
              gqlNode.copy(
                  directives = gqlNode.directives.sortedBy { it.score() }
              )
          )
        }
        is GQLFragmentDefinition -> {
          TransformResult.Replace(
              gqlNode.copy(
                  directives = gqlNode.directives.sortedBy { it.score() }
              )
          )
        }
        is GQLDirective -> {
          TransformResult.Replace(
              gqlNode.copy(
                  arguments = gqlNode.arguments?.copy(gqlNode.arguments!!.arguments.sortedBy { it.score() })
              )
          )
        }
        else -> TransformResult.Continue
      }
    }

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