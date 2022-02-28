package com.apollographql.apollo3.ast.transformation

import com.apollographql.apollo3.ast.GQLArgument
import com.apollographql.apollo3.ast.GQLArguments
import com.apollographql.apollo3.ast.GQLDirective
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLNode
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.GQLStringValue
import com.apollographql.apollo3.ast.NodeTransformer
import com.apollographql.apollo3.ast.TransformResult
import com.apollographql.apollo3.ast.transform

fun addDeferLabel(operation: GQLOperationDefinition): GQLOperationDefinition {
  return operation.transform(
      AddDeferLabelsTransformer(
          prefix = operation.operationType,
          name = operation.name!!
      )
  ) as GQLOperationDefinition
}

fun addDeferLabel(fragmentDefinition: GQLFragmentDefinition): GQLFragmentDefinition {
  return fragmentDefinition.transform(
      AddDeferLabelsTransformer(
          prefix = "fragment",
          name = fragmentDefinition.name
      )
  ) as GQLFragmentDefinition
}

private class AddDeferLabelsTransformer(
    val prefix: String,
    val name: String,
) : NodeTransformer {
  private var index = 0
  override fun transform(node: GQLNode): TransformResult {
    if (node !is GQLDirective) {
      return TransformResult.Continue
    }
    if (node.name != "defer") {
      return TransformResult.Continue
    }
    if (node.arguments?.arguments?.any { it.name == "label" } == true) {
      // Label is already set
      return TransformResult.Continue
    }

    val label = "$prefix:$name:${index++}"
    val argumentValue = GQLStringValue(value = label, sourceLocation = node.sourceLocation)
    val argument = GQLArgument(name = "label", value = argumentValue, sourceLocation = node.sourceLocation)
    val arguments = node.arguments?.copy(arguments = node.arguments.arguments + argument)
        ?: GQLArguments(arguments = listOf(argument), sourceLocation = node.sourceLocation)
    val directive = node.copy(arguments = arguments)
    return TransformResult.Replace(directive)
  }
}
