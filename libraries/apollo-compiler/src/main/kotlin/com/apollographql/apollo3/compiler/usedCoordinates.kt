package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.ast.GQLDefinition
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.GQLSelection
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.definitionFromScope
import com.apollographql.apollo3.ast.rawType

internal fun usedCoordinates(schema: Schema, definitions: List<GQLDefinition>): Set<String> {
  return definitions.flatMap {
    when(it) {
      is GQLOperationDefinition -> {
        it.selectionSet.selections.usedCoordinates(schema, schema.rootTypeNameFor(it.operationType)) + it.variableDefinitions.map { it.type.rawType().name }
      }
      is GQLFragmentDefinition -> it.selectionSet.selections.usedCoordinates(schema, it.typeCondition.name)
      else -> error("")
    }
  }.toSet()
}

private fun List<GQLSelection>.usedCoordinates(schema: Schema, parentType: String): Set<String> {
  return flatMap {
    when(it) {
      is GQLField -> {
        val fieldType = it.definitionFromScope(schema, parentType)
        val result = mutableSetOf("$parentType.${it.name}")
        if (fieldType == null) {
          // This happens if some of the queries are badly formed
          // In that case, codegen is going to fail later on
          return@flatMap result
        }

        result.addAll(it.selectionSet?.selections.orEmpty().usedCoordinates(schema, fieldType.type.rawType().name))
        result
      }
      is GQLInlineFragment -> it.selectionSet.selections.usedCoordinates(schema, it.typeCondition.name)
      is GQLFragmentSpread -> emptySet() // already handled, no need to deep dive
    }
  }.toSet() + parentType
}
