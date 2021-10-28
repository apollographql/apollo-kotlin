package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.OperationIdGenerator
import com.apollographql.apollo.compiler.fromJson
import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import com.apollographql.apollo.gradle.internal.SchemaDownloader.cast
import graphql.language.Argument
import graphql.language.AstPrinter
import graphql.language.AstTransformer
import graphql.language.Definition
import graphql.language.Directive
import graphql.language.Document
import graphql.language.Field
import graphql.language.FloatValue
import graphql.language.FragmentDefinition
import graphql.language.FragmentSpread
import graphql.language.InlineFragment
import graphql.language.IntValue
import graphql.language.Node
import graphql.language.NodeVisitorStub
import graphql.language.OperationDefinition
import graphql.language.Selection
import graphql.language.SelectionSet
import graphql.language.StringValue
import graphql.language.VariableDefinition
import graphql.parser.Parser
import graphql.util.TraversalControl
import graphql.util.TraverserContext
import graphql.util.TreeTransformerUtil.changeNode
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.math.BigInteger

private fun Definition<*>.score(): String {
  // Fragments always go first
  return when (this) {
    is FragmentDefinition -> "a$name"
    is OperationDefinition -> "b$name"
    else -> error("Cannot sort Definition '$this'")
  }
}

private fun VariableDefinition.score(): String {
  return this.name
}

private fun Selection<*>.score(): String {
  return when (this) {
    is Field -> "a$name"
    is FragmentSpread -> "b$name"
    is InlineFragment -> "c" // apollo-tooling doesn't sort inline fragments
    else -> error("Cannot sort Selection '$this'")
  }
}

private fun Directive.score() = name

private fun Argument.score() = name

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

private fun <T : Node<*>> join(nodes: List<T>, delim: String, nodeMethod: (Node<*>)->String, prefix: String = "", suffix: String = ""): String {
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
 *
 * It heavily uses reflection because this isn't public API in graphql-java
 */
private fun printDocument(document: Document): String {
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

  internal fun String.normalize(): String {
    val document = Parser.parse(this)

    // From https://github.com/apollographql/apollo-tooling/blob/6d69f226c2e2c54b4fc0de6394d813bddfb54694/packages/apollo-graphql/src/operationId.ts#L84

    // No need to "Drop unused definition" as we include only one operation

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

    /**
     * Algorithm taken from https://github.com/apollographql/apollo-tooling/blob/6d69f226c2e2c54b4fc0de6394d813bddfb54694/packages/apollo-graphql/src/transforms.ts#L102
     * It doesn't match exactly what graphql-java does
     */
    val sortedDocument = AstTransformer().transform(hiddenLiterals, object : NodeVisitorStub() {
      override fun visitDocument(node: Document, context: TraverserContext<Node<*>>): TraversalControl {
        return changeNode(context, node.transform {
          it.definitions(node.definitions.sortedBy { it.score() })
        })
      }

      override fun visitOperationDefinition(node: OperationDefinition, context: TraverserContext<Node<*>>?): TraversalControl {
        return changeNode(context, node.transform {
          it.variableDefinitions(node.variableDefinitions.sortedBy { it.score() })
        })
      }

      override fun visitSelectionSet(node: SelectionSet, context: TraverserContext<Node<*>>): TraversalControl {
        return changeNode(context, node.transform {
          it.selections(node.selections.sortedBy { it.score() })
        })
      }

      override fun visitField(node: Field, context: TraverserContext<Node<*>>?): TraversalControl {
        return changeNode(context, node.transform {
          it.arguments(node.arguments.sortedBy { it.score() })
        })
      }

      override fun visitFragmentSpread(node: FragmentSpread, context: TraverserContext<Node<*>>): TraversalControl {
        return changeNode(context, node.transform {
          it.directives(node.directives.sortedBy { it.score() })
        })
      }

      override fun visitInlineFragment(node: InlineFragment, context: TraverserContext<Node<*>>?): TraversalControl {
        return changeNode(context, node.transform {
          it.directives(node.directives.sortedBy { it.score() })
        })
      }

      override fun visitFragmentDefinition(node: FragmentDefinition, context: TraverserContext<Node<*>>): TraversalControl {
        return changeNode(context, node.transform {
          it.directives(node.directives.sortedBy { it.score() })
        })
      }

      override fun visitDirective(node: Directive, context: TraverserContext<Node<*>>?): TraversalControl {
        return changeNode(context, node.transform {
          it.arguments(node.arguments.sortedBy { it.score() })
        })
      }
    })

    val minimized = printDocument(sortedDocument as Document)
        .replace(Regex("\\s+"), " ")
        .replace(Regex("([^_a-zA-Z0-9]) ")) { it.groupValues[1] }
        .replace(Regex(" ([^_a-zA-Z0-9])")) { it.groupValues[1] }

    return minimized
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