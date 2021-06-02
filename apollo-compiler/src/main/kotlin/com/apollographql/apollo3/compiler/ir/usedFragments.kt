package com.apollographql.apollo3.compiler.ir

import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLSelection
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.definitionFromScope
import com.apollographql.apollo3.ast.leafType

internal fun usedFragments(
    schema: Schema,
    allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    selections: List<GQLSelection>,
    rawTypename: String,
): Set<String> {
  return selections.flatMap {
    when (it) {
      is GQLField -> {
        val fieldDefinition = it.definitionFromScope(schema, rawTypename)!!
        usedFragments(schema, allFragmentDefinitions, it.selectionSet?.selections ?: emptyList(), fieldDefinition.type.leafType().name)
      }
      is GQLInlineFragment -> {
        usedFragments(schema, allFragmentDefinitions, it.selectionSet.selections, it.typeCondition.name)
      }
      is GQLFragmentSpread -> {
        val fragmentDefinition = allFragmentDefinitions[it.name]!!
        usedFragments(schema, allFragmentDefinitions, fragmentDefinition.selectionSet.selections, fragmentDefinition.typeCondition.name) + it.name
      }
    }
  }.toSet()
}