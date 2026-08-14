package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLFragmentSpread
import com.apollographql.apollo.ast.GQLInlineFragment
import com.apollographql.apollo.ast.GQLSelection
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.definitionFromScope
import com.apollographql.apollo.ast.rawType

/**
 * Returns the names of the fragments used by [selections], transitively.
 *
 * @param cache the fragments used by a given fragment, by fragment name. The result for a fragment only depends on the
 * fragment definition, so it can be reused across operations. Pass the same map for all the operations of a compilation
 * unit: without it, a fragment spread by n operations is walked n times, and nested spreads multiply that.
 */
internal fun usedFragments(
    schema: Schema,
    allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    selections: List<GQLSelection>,
    rawTypename: String,
    cache: MutableMap<String, Set<String>> = mutableMapOf(),
): Set<String> {
  return selections.flatMap {
    when (it) {
      is GQLField -> {
        val fieldDefinition = it.definitionFromScope(schema, rawTypename)!!
        usedFragments(schema, allFragmentDefinitions, it.selections, fieldDefinition.type.rawType().name, cache)
      }
      is GQLInlineFragment -> {
        val tc = it.typeCondition?.name ?: rawTypename
        usedFragments(schema, allFragmentDefinitions, it.selections, tc, cache)
      }
      is GQLFragmentSpread -> {
        cache.getOrPut(it.name) {
          val fragmentDefinition = allFragmentDefinitions[it.name]!!
          usedFragments(schema, allFragmentDefinitions, fragmentDefinition.selections, fragmentDefinition.typeCondition.name, cache) + it.name
        }
      }
    }
  }.toSet()
}