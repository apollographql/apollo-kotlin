package com.apollographql.apollo3.ast

/**
 * A variable used in a [GQLValue]
 */
class VariableReference(
    val variable: GQLVariableValue,
    val expectedType: GQLType
)