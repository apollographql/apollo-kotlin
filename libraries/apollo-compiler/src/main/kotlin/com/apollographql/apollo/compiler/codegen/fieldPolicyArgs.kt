package com.apollographql.apollo.compiler.codegen

import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLStringValue
import com.apollographql.apollo.ast.GQLTypeDefinition
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.SourceAwareException
import com.apollographql.apollo.ast.parseAsGQLSelections
import okio.Buffer

internal fun GQLTypeDefinition.keyArgs(
    fieldName: String,
    schema: Schema,
): Set<String> = fieldPolicyArgs(Schema.FIELD_POLICY_KEY_ARGS, fieldName, schema)

private fun GQLTypeDefinition.fieldPolicyArgs(argumentName: String, fieldName: String, schema: Schema): Set<String> {
  return directives.filter { schema.originalDirectiveName(it.name) == Schema.FIELD_POLICY }.filter {
    (it.arguments.single { it.name == Schema.FIELD_POLICY_FOR_FIELD }.value as GQLStringValue).value == fieldName
  }.flatMap {
    val argValue = it.arguments.singleOrNull { it.name == argumentName }?.value ?: return emptySet()

    if (argValue !is GQLStringValue) {
      throw SourceAwareException("Apollo: no $argumentName found or wrong $argumentName type", it.sourceLocation)
    }

    Buffer().writeUtf8(argValue.value)
        .parseAsGQLSelections()
        .value
        ?.map {
          if (it !is GQLField) {
            throw SourceAwareException("Apollo: fragments are not supported in $argumentName", it.sourceLocation)
          }
          if (it.selections.isNotEmpty()) {
            throw SourceAwareException("Apollo: composite fields are not supported in $argumentName", it.sourceLocation)
          }
          it.name
        } ?: throw SourceAwareException("Apollo: $argumentName should be a selectionSet", it.sourceLocation)
  }.toSet()
}
