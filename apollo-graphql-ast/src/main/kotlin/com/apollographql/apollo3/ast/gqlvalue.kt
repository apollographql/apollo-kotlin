package com.apollographql.apollo3.ast

/**
 * For a [GQLValue] used in input position, validate that it can be coerced to [expectedType] and coerce it at the same time.
 *
 * Additionally, it will collect any variables used in this [GQLValue]
 *
 * This can happen in a lot of places. Places that
 * - variable defaultValue (executable)
 * - field argument value (executable)
 * - field argument defaultValue (schema)
 * - input field defaultValue (schema)
 */
fun GQLValue.validateAndCoerce(expectedType: GQLType, typeDefinitions: Map<String, GQLTypeDefinition>): InputValueValidationResult {
  return InputValueValidationScope(typeDefinitions).validateAndCoerce(this, expectedType)
}

