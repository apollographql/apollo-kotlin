package com.apollographql.apollo.ast.internal

import com.apollographql.apollo.ast.*

/**
 * Returns true if the two nodes are semantically equal, which ignores the source location and the description.
 */
internal fun GQLNode.semanticEquals(other: GQLNode?): Boolean {
  if (other == null) return false

  return toSemanticSdl() == other.toSemanticSdl()
}

/**
 * Normalizes the given [GQLNode].
 *
 * Two nodes with the same normalized representation behave the same during execution.
 *
 * Especially, descriptions are removed and field, inputFields, arguments, etc.. are sorted lexicographically.
 */
internal fun GQLNode.toSemanticSdl(): String {
  return transform2 { node ->
    when (node) {
      is GQLInputValueDefinition -> node.copy(description = null)
      is GQLObjectTypeDefinition -> node.copy(description = null, fields = node.fields.sortedBy { it.name })
      is GQLScalarTypeDefinition -> node.copy(description = null)
      is GQLInputObjectTypeDefinition -> node.copy(description = null, inputFields = node.inputFields.sortedBy { it.name })
      is GQLUnionTypeDefinition -> node.copy(description = null, memberTypes = node.memberTypes.sortedBy { it.name })
      is GQLEnumTypeDefinition -> node.copy(description = null, enumValues = node.enumValues.sortedBy { it.name })
      is GQLInterfaceTypeDefinition -> node.copy(description = null, fields = node.fields.sortedBy { it.name })
      is GQLFieldDefinition -> node.copy(description = null, arguments = node.arguments.sortedBy { it.name })
      is GQLDirectiveDefinition -> node.copy(description = null, arguments = node.arguments.sortedBy { it.name })
      is GQLEnumValueDefinition -> node.copy(description = null)
      is GQLFragmentDefinition -> node.copy(description = null)
      is GQLOperationDefinition -> node.copy(description = null)
      is GQLSchemaDefinition -> node.copy(description = null)
      else -> node
    }
  }!!.toUtf8("").replace(Regex("[\\n ]+"), " ").trim()
}
