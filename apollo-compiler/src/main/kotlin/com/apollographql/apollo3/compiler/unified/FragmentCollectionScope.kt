package com.apollographql.apollo3.compiler.unified

import com.apollographql.apollo3.compiler.frontend.GQLField
import com.apollographql.apollo3.compiler.frontend.GQLFragmentDefinition
import com.apollographql.apollo3.compiler.frontend.GQLFragmentSpread
import com.apollographql.apollo3.compiler.frontend.GQLInlineFragment
import com.apollographql.apollo3.compiler.frontend.GQLSelection

class FragmentCollectionScope(
    private val selections: List<GQLSelection>,
    private val baseType: String,
    private val allGQLFragmentDefinitions: Map<String, GQLFragmentDefinition>,
) {

  class Result(
      val typeSet: Set<TypeSet>,
      val namedFragments: Set<CollectedFragment>,
  )

  /**
   * @param condition: the condition of the fragment. This is not used right now but will be needed the day we want to support @include
   * directives on named fragments. The condition also includes the typeset
   */
  class CollectedFragment(
      val name: String,
      val condition: BooleanExpression,
  )


  private var collectedNamedFragments = mutableSetOf<CollectedFragment>()
  private var collectedTypeSets = mutableSetOf<TypeSet>()

  /**
   * Collect
   *
   * Each individual set matches a path for which we need to potentially generate a model
   * TODO: this whole logic isn't reallty used, remove it
   *
   * {# Base type A
   *   ... on B {
   *     ... on C {
   *   }
   *   ... on B {
   *     ... on D {
   *   }
   * }
   *
   * Will return:
   * [
   *   [A]
   *   [A,B]
   *   [A,B,C]
   *   [A,B,D]
   * ]
   */
  fun collect(): Result {
    selections.collectInternal(setOf(baseType))
    return Result(collectedTypeSets, collectedNamedFragments)
  }

  private fun List<GQLSelection>.collectInternal(typeSet: TypeSet) {
    collectedTypeSets.add(typeSet)
    forEach {
      when (it) {
        is GQLField -> return@forEach
        is GQLInlineFragment -> it.selectionSet.selections.collectInternal(typeSet + it.typeCondition.name)
        is GQLFragmentSpread -> {
          val fragmentDefinition = allGQLFragmentDefinitions[it.name]!!
          collectedNamedFragments.add(
              CollectedFragment(
                  it.name,
                  BooleanExpression.And(typeSet.map { BooleanExpression.Type(it) }.toSet())
                      // TODO: allow @include directives on named fragments
                      //.and(it.directives.toBooleanExpression())
                      .simplify()
              )
          )
          fragmentDefinition.selectionSet.selections.collectInternal(typeSet + fragmentDefinition.typeCondition.name)
        }
      }
    }
  }
}
