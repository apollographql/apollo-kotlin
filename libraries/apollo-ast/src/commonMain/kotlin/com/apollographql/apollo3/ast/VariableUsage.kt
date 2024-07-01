package com.apollographql.apollo.ast

/**
 * A variable used in a [GQLValue]
 */
class VariableUsage(
    val variable: GQLVariableValue,
    val locationType: GQLType,
    val hasLocationDefaultValue: Boolean,
    val isOneOfInputField: Boolean,
)

/**
 * A variable that is inferred from its usages in fragments
 * This is used to create executable fragments
 */
class InferredVariable(
    val name: String,
    val type: GQLType,
)