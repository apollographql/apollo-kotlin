package com.apollographql.apollo.compiler.parser

import com.apollographql.apollo.compiler.parser.antlr.GraphQLParser
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.misc.Interval

object GraphQLDocumentSourceBuilder {

  val GraphQLParser.OperationDefinitionContext.graphQLDocumentSource: String
    get() {
      val operationType = operationType().text
      val operationName = NAME().text
      val variables = variableDefinitions()?.source ?: ""
      val fields = selectionSet()?.format(addTypeName = false)?.let { " {\n$it\n}" } ?: ""
      return "$operationType $operationName$variables$fields".withIndents
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
    val withTypeName = addTypeName && selection()?.find {
      it.field()?.fieldName()?.NAME()?.text == "__typename"
    } == null
    val fields = selection()?.mapNotNull { selection ->
      selection.field()?.source ?: selection?.inlineFragment()?.source ?: selection?.fragmentSpread()?.source
    }
    return fields?.joinToString(prefix = if (withTypeName) "__typename\n" else "", separator = "\n") ?: ""
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
      val fieldName = fieldName().source
      val directives = directives()?.source?.let { " $it" } ?: ""
      val arguments = arguments()?.source ?: ""
      val selectionSet = selectionSet()?.format()?.let { " {\n$it\n}" } ?: ""
      return "$fieldName$directives$arguments$selectionSet"
    }

  private val GraphQLParser.ArgumentsContext.source: String
    get() = argument().joinToString(separator = ", ", prefix = "(", postfix = ")") { it.source }

  private val GraphQLParser.ArgumentContext.source: String
    get() = valueOrVariable()?.value()?.source?.let { "${NAME().text}: $it" } ?: (this as ParserRuleContext).source

  private val GraphQLParser.ValueContext.source: String
    get() {
      return when (this) {
        is GraphQLParser.ArrayValueContext -> source
        is GraphQLParser.InlineInputTypeValueContext -> inlineInputType().source
        else -> (this as ParserRuleContext).source
      }
    }

  private val GraphQLParser.ArrayValueContext.source: String
    get() = arrayValueType().valueOrVariable().joinToString(separator = ", ", prefix = "[", postfix = "]") { it.source }

  private val GraphQLParser.InlineInputTypeContext.source: String
    get() = inlineInputTypeField().joinToString(separator = ", ", prefix = "{", postfix = "}") { ctx ->
      "${ctx.NAME().text}: ${ctx.valueOrVariable()?.variable()?.source ?: ctx.valueOrVariable().value().source}"
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
