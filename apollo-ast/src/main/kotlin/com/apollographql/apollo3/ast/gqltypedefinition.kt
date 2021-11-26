package com.apollographql.apollo3.ast

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.internal.buffer

// 5.5.2.3 Fragment spread is possible
internal fun GQLTypeDefinition.sharesPossibleTypesWith(other: GQLTypeDefinition, schema: Schema): Boolean {
  return schema.possibleTypes(this).intersect(schema.possibleTypes(other)).isNotEmpty()
}
fun GQLTypeDefinition.possibleTypes(schema: Schema): Set<String> {
  return schema.possibleTypes(this)
}

fun GQLTypeDefinition.isFieldNonNull(fieldName: String): Boolean {
  val directive = when (this) {
    is GQLObjectTypeDefinition -> directives
    is GQLInterfaceTypeDefinition -> directives
    else -> return false
  }.firstOrNull { it.name == "nonnull" }

  if (directive == null) {
    return false
  }

  val stringValue = (directive.arguments!!.arguments.first().value as GQLStringValue).value

  @OptIn(ApolloExperimental::class)
  val selections = stringValue.buffer().parseAsGQLSelections().valueAssertNoErrors()

  return selections.filterIsInstance<GQLField>()
      .map { it.name }
      .contains(fieldName)
}