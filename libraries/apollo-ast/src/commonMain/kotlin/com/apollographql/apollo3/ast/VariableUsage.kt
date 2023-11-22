package com.apollographql.apollo3.ast

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v4_0_0

/**
 * A variable used in a [GQLValue]
 */
class VariableUsage(
    val variable: GQLVariableValue,
    val locationType: GQLType,
    val hasLocationDefaultValue: Boolean,
    val isOneOfInputObject: Boolean,
) {
  @Deprecated(
      message = "Use the constructor with isOneOfInputObject instead",
      replaceWith = ReplaceWith("VariableUsage(variable, locationType, hasLocationDefaultValue, isOneOfInputObject = false)"),
      level = DeprecationLevel.ERROR
  )
  @ApolloDeprecatedSince(v4_0_0)
  constructor(
      variable: GQLVariableValue,
      locationType: GQLType,
      hasLocationDefaultValue: Boolean,
  ) : this(
      variable = variable,
      locationType = locationType,
      hasLocationDefaultValue = hasLocationDefaultValue,
      isOneOfInputObject = false,
  )
}

/**
 * A variable that is inferred from its usages in fragments
 * This is used to create executable fragments
 */
class InferredVariable(
    val name: String,
    val type: GQLType,
)