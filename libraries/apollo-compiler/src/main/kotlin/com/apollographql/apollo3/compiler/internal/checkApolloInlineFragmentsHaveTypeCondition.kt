@file:JvmName("-checkApolloInlineFragmentsHaveTypeCondition")

package com.apollographql.apollo.compiler.internal

import com.apollographql.apollo.ast.GQLDefinition
import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLInlineFragment
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.GQLSelection
import com.apollographql.apollo.ast.InlineFragmentWithoutTypeCondition
import com.apollographql.apollo.ast.Issue

/**
 * Checks that targetNames don't clash with other class name.
 * Note: case is ignored in comparison because we assume the file system is case-insensitive.
 */
internal fun checkApolloInlineFragmentsHaveTypeCondition(definitions: List<GQLDefinition>): List<Issue> {
  val issues = mutableListOf<Issue>()
  definitions.forEach {
    it.walk(issues)
  }

  return issues
}

private fun GQLDefinition.walk(issues: MutableList<Issue>) {
  when (this) {
    is GQLOperationDefinition -> selections.forEach { it.walk(issues) }
    is GQLFragmentDefinition -> selections.forEach { it.walk(issues) }
    else -> {}
  }
}


private fun GQLSelection.walk(issues: MutableList<Issue>) {
  when (this) {
    is GQLField -> {
      selections.forEach { it.walk(issues) }
    }

    is GQLInlineFragment -> {
      if (typeCondition == null) {
        issues.add(InlineFragmentWithoutTypeCondition("Inline fragments without a type condition are not supported. Add the parent type to inline fragment.", this.sourceLocation))
      }
    }

    else -> {}
  }
}
