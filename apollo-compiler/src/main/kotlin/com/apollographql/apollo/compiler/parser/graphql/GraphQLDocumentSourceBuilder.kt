package com.apollographql.apollo.compiler.parser.graphql

import com.apollographql.apollo.compiler.parser.antlr.GraphQLParser
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.misc.Interval

object GraphQLDocumentSourceBuilder {

  val GraphQLParser.OperationDefinitionContext.graphQLDocumentSource: String
    get() {
      val operationType = operationType().text
      val operationName = name().text
      val variables = variableDefinitions()?.source ?: ""
      val directives = directives()?.source?.let { "$it " } ?: ""
      val hasFragments = selectionSet().selection()?.mapNotNull { ctx -> ctx.fragmentSpread() }?.isNotEmpty() ?: false
      val fields = selectionSet()?.format(addTypeName = hasFragments)?.let { "{\n$it\n}" } ?: ""
      return "$operationType $operationName$variables $directives$fields".withIndents
    }

  val GraphQLParser.FragmentDefinitionContext.graphQLDocumentSource: String
    get() {
      val fragmentName = fragmentName().text
      val typeCondition = typeCondition().source
      val directives = directives()?.source?.let { "$it " } ?: ""
      val fields = selectionSet()?.format()?.let { "{\n$it\n}" } ?: ""
      return "fragment $fragmentName on $typeCondition $directives$fields".withIndents
    }

  private val GraphQLParser.VariableDefinitionsContext.source: String
    get() = variableDefinition().joinToString(separator = ", ", prefix = "(", postfix = ")") { it.source }

  private fun GraphQLParser.SelectionSetContext.format(addTypeName: Boolean = true): String {
    val withTypeName = addTypeName || selection()?.find {
      it.field()?.name()?.text == "__typename"
    } != null
    return selection()
        ?.filterNot { selection ->
          selection.field()?.name()?.text == "__typename"
        }
        ?.mapNotNull { selection ->
          selection.field()?.source ?: selection?.inlineFragment()?.source ?: selection?.fragmentSpread()?.source
        }
        ?.joinToString(
            prefix = if (withTypeName) "__typename\n" else "",
            separator = "\n"
        )
        ?: ""
  }

  private val GraphQLParser.InlineFragmentContext.source: String
    get() {
      val typeCondition = typeCondition().source
      val directives = directives()?.source?.let { "$it " } ?: ""
      val hasInlineFragments = selectionSet().selection()?.find { it.inlineFragment() != null } != null
      val fields = selectionSet().format(addTypeName = hasInlineFragments)
      return "... on $typeCondition $directives{\n$fields\n}"
    }

  private val GraphQLParser.FieldContext.source: String
    get() {
      val fieldName = (alias()?.source ?: "") + name().source
      val arguments = arguments()?.source ?: ""
      val directives = directives()?.source?.let { " $it" } ?: ""
      val selectionSet = selectionSet()?.format()?.let { " {\n$it\n}" } ?: ""
      return "$fieldName$arguments$directives$selectionSet"
    }

  private val GraphQLParser.ArgumentsContext.source: String
    get() = argument().joinToString(separator = ", ", prefix = "(", postfix = ")") { it.source }

  private val GraphQLParser.ArgumentContext.source: String
    get() = value()?.source?.let { "${name().text}: $it" } ?: (this as ParserRuleContext).source

  private val GraphQLParser.ValueContext.source: String
    get() {
      return when {
        listValue() != null -> listValue().source
        objectValue() != null -> objectValue().source
        else -> (this as ParserRuleContext).source
      }
    }

  private val GraphQLParser.ListValueContext.source: String
    get() = value().joinToString(separator = ", ", prefix = "[", postfix = "]") { it.source }

  private val GraphQLParser.ObjectValueContext.source: String
    get() = objectField().joinToString(separator = ", ", prefix = "{", postfix = "}") { ctx ->
      "${ctx.name().text}: ${ctx.value()?.variable()?.source ?: ctx.value().source}"
    }

  private val ParserRuleContext.source: String
    get() {
      val interval = Interval(start.startIndex, stop.stopIndex)
      return start.inputStream.getText(interval).trim()
    }

  private val String.withIndents: String
    get() {
      var indent = 0
      return lines().joinToString(separator = "\n") { line ->
        if (line.endsWith("}")) indent -= 2
        (" ".repeat(indent) + line).also {
          if (line.endsWith("{")) indent += 2
        }
      }
    }
}
