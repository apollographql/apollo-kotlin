package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.ast.GQLDefinition
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.GQLSelection
import com.apollographql.apollo3.ast.GQLUnionTypeDefinition
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
  return flatMap { selection ->
    when(selection) {
      is GQLField -> {
        val fieldType = selection.definitionFromScope(schema, parentType)
        val result = mutableSetOf<String>()
        if (fieldType == null) {
          // This happens if some of the queries are badly formed
          // In that case, codegen is going to fail later on
          return@flatMap result
        }

        schema.possibleTypes(parentType).map {
          schema.typeDefinition(it)
        }
            .filterIsInstance<GQLObjectTypeDefinition>()
            .forEach {
              /**
               * Add all possible types and their fields because the user might want to use any of them
               * GetHeroQuery.Data {
               *   hero = buildHuman {}  // or buildDroid {}
               * }
               */
              result.add(it.name)
              result.add("${it.name}.${selection.name}")
            }

        result.addAll(selection.selectionSet?.selections.orEmpty().usedCoordinates(schema, fieldType.type.rawType().name))
        result
      }
      is GQLInlineFragment -> selection.selectionSet.selections.usedCoordinates(schema, selection.typeCondition.name)
      is GQLFragmentSpread -> emptySet() // already handled, no need to deep dive
    }
  }.toSet() + parentType
}
