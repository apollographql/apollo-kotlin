package com.apollographql.apollo3.graphql.ast

class InputValueValidationResult(
    val coercedValue: GQLValue,
    val variableReferences: List<VariableReference>,
    val issues: List<Issue>
) {
  fun getOrThrow(): GQLValue {
    // Let warnings go through.
    // Especially deprecation warnings are ok.
    if (issues.any { it.severity == Issue.Severity.ERROR }) {
      throw SourceAwareException(issues.first().message, issues.first().sourceLocation)
    }
    return coercedValue
  }
}