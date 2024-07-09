package com.apollographql.apollo.ast

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.ast.Schema.Companion.NONNULL

// 5.5.2.3 Fragment spread is possible
internal fun GQLTypeDefinition.sharesPossibleTypesWith(other: GQLTypeDefinition, schema: Schema): Boolean {
  return schema.possibleTypes(this).intersect(schema.possibleTypes(other)).isNotEmpty()
}

fun GQLTypeDefinition.possibleTypes(schema: Schema): Set<String> {
  return schema.possibleTypes(this)
}

@ApolloInternal
fun GQLTypeDefinition.isFieldNonNull(fieldName: String, schema: Schema? = null): Boolean {
  val directive = when (this) {
    is GQLObjectTypeDefinition -> directives
    is GQLInterfaceTypeDefinition -> directives
    else -> return false
  }.firstOrNull { (schema?.originalDirectiveName(it.name) ?: it.name) == NONNULL }

  if (directive == null) {
    return false
  }

  val stringValue = (directive.arguments.first().value as GQLStringValue).value

  val selections = stringValue.parseAsGQLSelections().getOrThrow()

  return selections.filterIsInstance<GQLField>()
      .map { it.name }
      .contains(fieldName)
}

fun GQLTypeDefinition.isAbstract(): Boolean {
  return when (this) {
    is GQLUnionTypeDefinition,
    is GQLInterfaceTypeDefinition,
    -> true
    else -> false
  }
}
fun GQLTypeDefinition.implementsAbstractType(schema: Schema): Boolean {
  return schema.implementedTypes(name).any {
    schema.typeDefinition(it).isAbstract()
  }
}

fun GQLTypeDefinition.canHaveKeyFields(): Boolean {
  return when (this) {
    is GQLObjectTypeDefinition,
    is GQLInterfaceTypeDefinition,
    is GQLUnionTypeDefinition
    -> true
    else -> false
  }
}
