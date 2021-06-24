package com.apollographql.apollo3.ast.transformation

import com.apollographql.apollo3.ast.*

fun List<GQLSelection>.mergeTrivialInlineFragments(schema: Schema, rawTypename: String): List<GQLSelection> {
  return flatMap {
    when (it) {
      is GQLField -> {
        val parentType = it.definitionFromScope(schema, rawTypename)!!.type.leafType().name
        listOf(
            it.copy(
                selectionSet = it.selectionSet?.copy(
                    selections = it.selectionSet.selections.mergeTrivialInlineFragments(schema, parentType)
                )
            )
        )
      }
      is GQLFragmentSpread -> listOf(it)
      is GQLInlineFragment -> {
        if (it.typeCondition.name == rawTypename && it.directives.isEmpty()) {
          it.selectionSet.selections.mergeTrivialInlineFragments(schema, rawTypename)
        } else {
          listOf(
              it.copy(
                  selectionSet = it.selectionSet.copy(selections = it.selectionSet.selections.mergeTrivialInlineFragments(schema, it.typeCondition.name))
              )
          )
        }
      }
    }
  }
}