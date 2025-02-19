package com.apollographql.apollo.ast.internal

import com.apollographql.apollo.ast.*

/**
 * Returns true if the two nodes are semantically equal, which ignores the source location and the description.
 */
internal fun GQLNode.semanticEquals(other: GQLNode?): Boolean {
  if (other == null) return false

  return toSemanticSdl() == other.toSemanticSdl()
}

internal fun GQLNode.toSemanticSdl(): String {
  return transform2 { node ->
    when (node) {
      is GQLInputValueDefinition -> node.copy(description = null)
      is GQLObjectTypeDefinition -> node.copy(description = null)
      is GQLScalarTypeDefinition -> node.copy(description = null)
      is GQLInputObjectTypeDefinition -> node.copy(description = null)
      is GQLUnionTypeDefinition -> node.copy(description = null)
      is GQLEnumTypeDefinition -> node.copy(description = null)
      is GQLInterfaceTypeDefinition -> node.copy(description = null)
      is GQLFieldDefinition -> node.copy(description = null)
      is GQLDirectiveDefinition -> node.copy(description = null)
      is GQLEnumValueDefinition -> node.copy(description = null)
      is GQLFragmentDefinition -> node.copy(description = null)
      is GQLOperationDefinition -> node.copy(description = null)
      is GQLSchemaDefinition -> node.copy(description = null)
      else -> node
    }
  }!!.toUtf8("").replace(Regex("[\\n ]+"), " ").trim()
}
