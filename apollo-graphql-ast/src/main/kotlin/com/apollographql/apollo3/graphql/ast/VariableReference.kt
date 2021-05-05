package com.apollographql.apollo3.graphql.ast

/**
 * A variable used in a [GQLValue]
 */
class VariableReference(
    val variable: GQLVariableValue,
    val expectedType: GQLType
)