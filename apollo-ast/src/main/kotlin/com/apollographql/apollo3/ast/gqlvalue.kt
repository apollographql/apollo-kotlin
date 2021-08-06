package com.apollographql.apollo3.ast

import com.apollographql.apollo3.ast.internal.DefaultValidationScope
import com.apollographql.apollo3.ast.internal.ExecutableValidationScope2
import com.apollographql.apollo3.ast.internal.validateAndCoerceValue

/**
 * For a [GQLValue] used in input position, validate that it can be coerced to [expectedType] and coerce it at the same time.
 *
 * This should only be used in places where variables are available. For an example:
 * - variable defaultValue (executable)
 * - field argument value (executable)
 */
fun GQLValue.coerceInExecutableContextOrThrow(expectedType: GQLType, schema: Schema): GQLValue {
  val scope = ExecutableValidationScope2(schema)
  val coercedValue = scope.validateAndCoerceValue(this, expectedType)
  scope.issues.checkNoErrors()
  return coercedValue
}

/**
 * For a [GQLValue] used in input position, validate that it can be coerced to [expectedType] and coerce it at the same time.
 *
 * This should only be used in places where no variables are available. For an example:
 * - field argument defaultValue (schema)
 * - input field defaultValue (schema)
 * - directive argument values
 */
fun GQLValue.coerceInSchemaContextOrThrow(expectedType: GQLType, schema: Schema): GQLValue {
  val scope = DefaultValidationScope(schema)
  val coercedValue = scope.validateAndCoerceValue(this, expectedType)
  scope.issues.checkNoErrors()
  return coercedValue
}