package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.ast.GQLDefinition
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.GQLSelection
import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.internal.IssuesScope
import com.apollographql.apollo3.compiler.ir.toBooleanExpression


internal fun findConditionalFragments(definitions: List<GQLDefinition>): List<Issue> {
  val scope = object : IssuesScope {
    override val issues = mutableListOf<Issue>()
  }

  definitions.forEach {
    when (it) {
      is GQLOperationDefinition -> scope.findConditionalFragments(it.selectionSet.selections)
      is GQLFragmentDefinition -> scope.findConditionalFragments(it.selectionSet.selections)
    }
  }

  return scope.issues
}

/**
 * Fragments with @include/@skip directives are hard to generate as responseBased models because they would
 * need multiple shapes depending on the @include condition. This is not supported at the moment
 */
private fun IssuesScope.findConditionalFragments(selections: List<GQLSelection>) {
  selections.forEach {
    when (it) {
      is GQLField -> findConditionalFragments(it.selectionSet?.selections ?: emptyList())
      is GQLInlineFragment -> {
        if (it.directives.toBooleanExpression() != BooleanExpression.True) {
          issues.add(
              Issue.ConditionalFragment(
                  message = "'responseBased' models do not support @include/@skip directives on fragments",
                  sourceLocation = it.sourceLocation
              )
          )
        }
        findConditionalFragments(it.selectionSet.selections)
      }
      is GQLFragmentSpread -> {
        if (it.directives.toBooleanExpression() != BooleanExpression.True) {
          issues.add(
              Issue.ConditionalFragment(
                  message = "'responseBased' models do not support @include/@skip directives on fragments",
                  sourceLocation = it.sourceLocation
              )
          )
        }
      }
    }
  }
}


