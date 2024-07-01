package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLFragmentSpread
import com.apollographql.apollo.ast.GQLInlineFragment
import com.apollographql.apollo.ast.GQLSelection
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.definitionFromScope
import com.apollographql.apollo.ast.rawType

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
        usedFragments(schema, allFragmentDefinitions, it.selections, fieldDefinition.type.rawType().name)
      }
      is GQLInlineFragment -> {
        val tc = it.typeCondition?.name ?: rawTypename
        usedFragments(schema, allFragmentDefinitions, it.selections, tc)
      }
      is GQLFragmentSpread -> {
        val fragmentDefinition = allFragmentDefinitions[it.name]!!
        usedFragments(schema, allFragmentDefinitions, fragmentDefinition.selections, fragmentDefinition.typeCondition.name) + it.name
      }
    }
  }.toSet()
}