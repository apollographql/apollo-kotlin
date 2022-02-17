package com.apollographql.apollo3.ast

/**
 * A variable used in a [GQLValue]
 */
class VariableUsage(
    val variable: GQLVariableValue,
    val locationType: GQLType,
    val hasLocationDefaultValue: Boolean
)