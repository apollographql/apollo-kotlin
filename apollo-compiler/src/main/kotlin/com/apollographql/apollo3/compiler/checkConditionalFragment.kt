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

internal fun checkConditionalFragments(definitions: List<GQLDefinition>): List<Issue> {
  val scope = object : IssuesScope {
    override val issues = mutableListOf<Issue>()
  }

  definitions.forEach {
    when (it) {
      is GQLOperationDefinition -> scope.checkConditionalFragments(it.selectionSet.selections)
      is GQLFragmentDefinition -> scope.checkConditionalFragments(it.selectionSet.selections)
    }
  }

  return scope.issues
}

/**
 * Fragments with @include/@skip or @defer directives are hard to generate as responseBased models because they would
 * need multiple shapes depending on the condition. This is not supported at the moment
 */
private fun IssuesScope.checkConditionalFragments(selections: List<GQLSelection>) {
  selections.forEach {
    when (it) {
      is GQLField -> checkConditionalFragments(it.selectionSet?.selections ?: emptyList())
      is GQLInlineFragment -> {
        if (it.directives.toBooleanExpression() != BooleanExpression.True) {
          issues.add(
              Issue.ConditionalFragment(
                  message = "'responseBased' and 'operationBased2' models do not support @include/@skip and @defer directives on fragments",
                  sourceLocation = it.sourceLocation
              )
          )
        }
        checkConditionalFragments(it.selectionSet.selections)
      }
      is GQLFragmentSpread -> {
        if (it.directives.toBooleanExpression() != BooleanExpression.True) {
          issues.add(
              Issue.ConditionalFragment(
                  message = "'responseBased' and 'operationBased2' models do not support @include/@skip and @defer directives on fragments",
                  sourceLocation = it.sourceLocation
              )
          )
        }
      }
    }
  }
}


