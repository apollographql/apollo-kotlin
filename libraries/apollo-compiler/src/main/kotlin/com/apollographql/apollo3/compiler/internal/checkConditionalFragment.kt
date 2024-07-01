package com.apollographql.apollo.compiler.internal

import com.apollographql.apollo.ast.ConditionalFragment
import com.apollographql.apollo.ast.GQLDefinition
import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLFragmentSpread
import com.apollographql.apollo.ast.GQLInlineFragment
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.GQLSelection
import com.apollographql.apollo.ast.Issue
import com.apollographql.apollo.compiler.ir.BooleanExpression
import com.apollographql.apollo.compiler.ir.toBooleanExpression

internal fun checkConditionalFragments(definitions: List<GQLDefinition>): List<Issue> {
  val issues = mutableListOf<Issue>()

  definitions.forEach {
    when (it) {
      is GQLOperationDefinition -> checkConditionalFragments(issues, it.selections)
      is GQLFragmentDefinition -> checkConditionalFragments(issues, it.selections)
      else -> {}
    }
  }

  return issues
}

/**
 * Fragments with @include/@skip or @defer directives are hard to generate as responseBased models because they would
 * need multiple shapes depending on the condition. This is not supported at the moment
 */
private fun checkConditionalFragments(issues: MutableList<Issue>, selections: List<GQLSelection>) {
  selections.forEach {
    when (it) {
      is GQLField -> checkConditionalFragments(issues, it.selections)
      is GQLInlineFragment -> {
        if (it.directives.toBooleanExpression() != BooleanExpression.True) {
          issues.add(
              ConditionalFragment(
                  message = "'responseBased' and 'experimental_operationBasedWithInterfaces' models do not support @include/@skip and @defer directives on fragments",
                  sourceLocation = it.sourceLocation
              )
          )
        }
        checkConditionalFragments(issues, it.selections)
      }
      is GQLFragmentSpread -> {
        if (it.directives.toBooleanExpression() != BooleanExpression.True) {
          issues.add(
              ConditionalFragment(
                  message = "'responseBased' and 'experimental_operationBasedWithInterfaces' models do not support @include/@skip and @defer directives on fragments",
                  sourceLocation = it.sourceLocation
              )
          )
        }
      }
    }
  }
}


